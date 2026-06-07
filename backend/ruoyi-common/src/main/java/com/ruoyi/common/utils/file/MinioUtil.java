package com.ruoyi.common.utils.file;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.MinioConfig;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;

/**
 * MinIO 文件上传工具类
 *
 * @author qixiaoxia
 */
public class MinioUtil
{
    /**
     * 获取默认存储桶名称
     */
    public static String getBucketName()
    {
        return SpringUtils.getBean(MinioConfig.class).getBucketName();
    }
    /**
     * 上传文件到 MinIO
     *
     * @param bucketName 桶名称
     * @param fileName 文件路径名 (如 2024/06/01/file_123.png)
     * @param multipartFile 上传文件
     * @return 文件访问 URL (不含签名参数)
     * @throws IOException
     */
    public static String uploadFile(String bucketName, String fileName, MultipartFile multipartFile) throws IOException
    {
        MinioClient minioClient = SpringUtils.getBean(MinioClient.class);
        try (InputStream inputStream = multipartFile.getInputStream())
        {
            // 确保存储桶存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found)
            {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(inputStream, multipartFile.getSize(), -1)
                    .contentType(multipartFile.getContentType())
                    .build());

            // 生成预签名 URL 并去掉查询参数
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .method(Method.GET)
                            .build());
            url = url.substring(0, url.indexOf('?'));
            return ServletUtils.urlDecode(url);
        }
        catch (Exception e)
        {
            throw new IOException("MinIO 文件上传失败: " + e.getMessage(), e);
        }
    }
}
