#!/usr/bin/env python3
"""
修复企小侠MES数据库中的 UTF-8 双重编码问题。

问题：SQL 初始化文件在 MySQL 客户端 charset=latin1 时导入，
导致 UTF-8 字节被误读为 Windows-1252 字符后再次编码为 utf8mb4 存储。

修复原理：
  garbled_str.encode('cp1252').decode('utf-8')
  1. 将 utf8mb4 字符编码回 Windows-1252 字节（还原原始 UTF-8 字节）
  2. 将这些字节按 UTF-8 解码为正确的 Unicode 字符串

ASCII 字符在此转换下不变，因此不会损坏英文数据。

使用方法：
  python3 backend/scripts/fix_encoding.py [--dry-run] [--table TABLE_NAME]

  不加 --dry-run 会实际更新数据库。
  加 --table 只处理指定表，否则处理所有表。
"""

import argparse
import sys

try:
    import pymysql
except ImportError:
    print("请先安装 pymysql: pip3 install pymysql")
    sys.exit(1)

# 数据库连接配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3307,
    'user': 'root',
    'password': 'qxx123456',
    'database': 'mes',
    'charset': 'utf8mb4',
}

# 需要跳过的非文本表（或不需要修复的表）
SKIP_TABLES = set()


def get_text_columns(cursor):
    """获取所有文本类型列"""
    cursor.execute("""
        SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = %s
          AND CHARACTER_SET_NAME IS NOT NULL
          AND DATA_TYPE IN ('varchar', 'char', 'text', 'mediumtext', 'longtext')
        ORDER BY TABLE_NAME, ORDINAL_POSITION
    """, (DB_CONFIG['database'],))

    columns = []
    for row in cursor.fetchall():
        table, column, dtype = row['TABLE_NAME'], row['COLUMN_NAME'], row['DATA_TYPE']
        if table not in SKIP_TABLES:
            columns.append((table, column, dtype))
    return columns


def get_primary_key(cursor, table):
    """获取表的主键列名"""
    cursor.execute("""
        SELECT COLUMN_NAME
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = %s
          AND TABLE_NAME = %s
          AND CONSTRAINT_NAME = 'PRIMARY'
        ORDER BY ORDINAL_POSITION
        LIMIT 1
    """, (DB_CONFIG['database'], table))
    row = cursor.fetchone()
    return row['COLUMN_NAME'] if row else None


def _char_to_original_byte(ch):
    """
    将乱码字符映射回原始单字节值。

    MySQL 的 latin1 字符集基于 cp1252，但对 cp1252 未定义的码点
    (如 C1 控制字符 0x80-0x9F) 会回退到 ISO-8859-1 的映射。

    因此需要先用 cp1252 尝试，失败后再用 latin1。
    """
    # 先尝试 cp1252（处理如 U+201D→0x94 等 Windows 特有字符）
    try:
        return ch.encode('cp1252')[0]
    except (UnicodeEncodeError, LookupError):
        pass
    # 回退到 latin1（处理如 U+008F→0x8F 等 C1 控制字符）
    try:
        return ch.encode('latin1')[0]
    except (UnicodeEncodeError, LookupError):
        pass
    return None


def fix_string(value):
    """
    反转 UTF-8 双重编码。
    逐字符映射回原始字节，再按 UTF-8 解码为正确的 Unicode 字符串。
    如果转换失败，返回原值。
    """
    if value is None:
        return None
    if not isinstance(value, str):
        return value
    try:
        byte_vals = []
        for ch in value:
            b = _char_to_original_byte(ch)
            if b is None:
                return value  # 无法映射的字符，返回原值
            byte_vals.append(b)
        fixed = bytes(byte_vals).decode('utf-8')
        return fixed
    except (UnicodeDecodeError, UnicodeEncodeError):
        return value


def needs_fix(value):
    """检查字符串是否需要修复（含有需要修复的非ASCII字符）"""
    if value is None:
        return False
    # 如果字符串完全是 ASCII，不需要修复（转换后不变）
    try:
        value.encode('ascii')
        return False  # 纯 ASCII，跳过
    except UnicodeEncodeError:
        pass

    # 尝试修复，看结果是否不同
    try:
        byte_vals = []
        for ch in value:
            b = _char_to_original_byte(ch)
            if b is None:
                return False
            byte_vals.append(b)
        fixed = bytes(byte_vals).decode('utf-8')
        return fixed != value
    except (UnicodeDecodeError, UnicodeEncodeError):
        return False


def main():
    parser = argparse.ArgumentParser(description='修复 MySQL 数据库中的 UTF-8 双重编码')
    parser.add_argument('--dry-run', action='store_true', help='仅检查不实际修改')
    parser.add_argument('--table', type=str, help='只处理指定表')
    args = parser.parse_args()

    print(f"连接数据库: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
    conn = pymysql.connect(**DB_CONFIG, cursorclass=pymysql.cursors.DictCursor)

    try:
        with conn.cursor() as cursor:
            columns = get_text_columns(cursor)
            if args.table:
                columns = [(t, c, d) for t, c, d in columns if t == args.table]
                if not columns:
                    print(f"表 '{args.table}' 没有找到文本列或不存在")
                    return

            print(f"找到 {len(columns)} 个文本列")

            # 按表分组
            table_columns = {}
            for table, column, dtype in columns:
                table_columns.setdefault(table, []).append((column, dtype))

            total_fixed = 0
            total_checked = 0

            for table, cols in table_columns.items():
                pk = get_primary_key(cursor, table)
                if pk is None:
                    print(f"\n⚠️  表 '{table}' 没有主键，跳过")
                    continue

                print(f"\n处理表: {table} (主键: {pk}, {len(cols)} 列)")

                # 查询所有行
                col_names = [c[0] for c in cols]
                select_cols = [pk] + col_names
                query = f"SELECT {', '.join(f'`{c}`' for c in select_cols)} FROM `{table}`"
                cursor.execute(query)
                rows = cursor.fetchall()

                table_fixed = 0
                for row in rows:
                    pk_val = row[pk]
                    updates = {}

                    for col_name, _ in cols:
                        old_val = row[col_name]
                        if old_val and needs_fix(old_val):
                            fixed_val = fix_string(old_val)
                            if fixed_val != old_val:
                                updates[col_name] = fixed_val

                    if updates:
                        total_checked += len(updates)
                        if args.dry_run:
                            # 显示前几个修复预览
                            for col_name, new_val in updates.items():
                                old_val = row[col_name]
                                if len(old_val) < 50:
                                    print(f"  [{pk}={pk_val}] {col_name}: '{old_val}' → '{new_val}'")
                                else:
                                    print(f"  [{pk}={pk_val}] {col_name}: '{old_val[:30]}...' → '{new_val[:30]}...'")
                        else:
                            set_clause = ', '.join(f'`{c}` = %s' for c in updates.keys())
                            values = list(updates.values()) + [pk_val]
                            update_sql = f"UPDATE `{table}` SET {set_clause} WHERE `{pk}` = %s"
                            cursor.execute(update_sql, values)
                            table_fixed += len(updates)
                            total_fixed += len(updates)

                if args.dry_run:
                    print(f"  需要修复: {table_fixed} 个字段")
                else:
                    print(f"  已修复: {table_fixed} 个字段")

        if args.dry_run:
            print(f"\n🔍 预览模式: 共需修复 {total_checked} 个字段")
            print("去掉 --dry-run 参数以实际执行修复。")
        else:
            conn.commit()
            print(f"\n✅ 修复完成: 共修复 {total_fixed} 个字段")
            if total_fixed > 0:
                print("请重启后端服务以使修复生效。")

    except Exception as e:
        conn.rollback()
        print(f"\n❌ 错误: {e}")
        raise
    finally:
        conn.close()


if __name__ == '__main__':
    main()
