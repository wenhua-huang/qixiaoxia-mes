#!/usr/bin/env python3
"""
获取企小侠MES后端 API 的认证 token。

自动完成 captcha 获取 → Redis 读取验证码 → 登录 → 输出 token 的全流程。

用法:
  # 仅输出 token（方便 export TOKEN=$(python3 backend/scripts/get_token.py)）
  python3 backend/scripts/get_token.py

  # 输出完整的 Authorization header（方便 curl 使用）
  python3 backend/scripts/get_token.py --print-header

  # 指定凭据
  python3 backend/scripts/get_token.py --user admin --password admin123

  # 输出 token + 直接测试 API
  python3 backend/scripts/get_token.py --test

依赖: pip3 install requests redis
"""

import argparse
import sys
import os

# 后端 API 地址
BASE_URL = os.environ.get('QXX_API_URL', 'http://localhost:8081')

# Redis 配置
REDIS_CONFIG = {
    'host': os.environ.get('QXX_REDIS_HOST', 'localhost'),
    'port': int(os.environ.get('QXX_REDIS_PORT', '6380')),
    'password': os.environ.get('QXX_REDIS_PASSWORD', 'qxx123456'),
}

# 默认登录凭据
DEFAULT_USER = 'admin'
DEFAULT_PASSWORD = 'admin123'


def get_token(user: str, password: str) -> str:
    """获取认证 token"""

    # 步骤 1: 获取验证码
    try:
        import requests
    except ImportError:
        sys.exit('请安装 requests: pip3 install requests')

    r = requests.get(f'{BASE_URL}/captchaImage', timeout=10)
    r.raise_for_status()
    captcha = r.json()
    uuid = captcha['uuid']

    # 步骤 2: 从 Redis 读取验证码
    try:
        import redis
    except ImportError:
        sys.exit('请安装 redis: pip3 install redis')

    rds = redis.Redis(**REDIS_CONFIG, decode_responses=True)
    code = rds.get(f'captcha_codes:{uuid}')
    if code is None:
        sys.exit(f'验证码未找到。uuid={uuid}，key=captcha_codes:{uuid}')
    code = code.strip('"')  # Redis 存储值可能带引号

    # 步骤 3: 登录获取 token
    login_data = {
        'username': user,
        'password': password,
        'code': code,
        'uuid': uuid,
    }
    r = requests.post(f'{BASE_URL}/login', json=login_data, timeout=10)
    r.raise_for_status()
    resp = r.json()

    if resp.get('code') != 200:
        sys.exit(f"登录失败: {resp.get('msg', '未知错误')}")

    return resp['token']


def main():
    parser = argparse.ArgumentParser(description='获取企小侠MES API 认证 token')
    parser.add_argument('--user', '-u', default=DEFAULT_USER, help=f'用户名 (默认: {DEFAULT_USER})')
    parser.add_argument('--password', '-p', default=DEFAULT_PASSWORD, help='密码')
    parser.add_argument('--print-header', '-H', action='store_true', help='输出完整 Authorization header')
    parser.add_argument('--test', '-t', action='store_true', help='获取 token 后测试 /getInfo API')
    args = parser.parse_args()

    print(f'🔐 正在登录 {BASE_URL} (用户: {args.user})...', file=sys.stderr)
    token = get_token(args.user, args.password)
    print('✅ 登录成功', file=sys.stderr)

    if args.print_header:
        print(f'Authorization: Bearer {token}')
    elif args.test:
        print(f'# Token: {token[:50]}...')
        print(f'# 测试 /getInfo ...')
        try:
            import requests
            r = requests.get(f'{BASE_URL}/getInfo', headers={'Authorization': f'Bearer {token}'}, timeout=10)
            data = r.json()
            if data.get('code') == 200:
                u = data['user']
                print(f'  nickName: {u["nickName"]}')
                print(f'  deptName: {u["dept"]["deptName"]}')
                print(f'  roleName: {u["roles"][0]["roleName"]}')
            else:
                print(f'  API 错误: {data.get("msg")}')
        except Exception as e:
            print(f'  测试失败: {e}')
    else:
        print(token)


if __name__ == '__main__':
    main()
