module "image_bucket" {
  source  = "terraform-aws-modules/s3-bucket/aws"
  version = "~> 5.7.0"

  bucket = "sesac-hello-pet-image-bucket"

  # 버킷 삭제시 내용 강제 삭제
  force_destroy = true

  attach_policy = true
  # policy = jsonencode(
  #   {
  #     "Version": "2012-10-17",
  #     "Statement": [
  #       {
  #         "Sid": "PublicReadGetObject",
  #         "Effect": "Allow",
  #         "Principal": "*",
  #         "Action": "s3:GetObject",
  #         "Resource": "${module.image_bucket.s3_bucket_arn}/*"
  #       }
  #     ]
  #   })
}
