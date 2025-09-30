terraform {
  required_version = ">= 1.2"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.5"
    }
    local = {
      source  = "hashicorp/local"
      version = "2.5.3"
    }
  }
}

# 기본 AWS 리전을 설정합니다. (ap-notrheast-2: 서울)
provider "aws" {
  region = "ap-northeast-2"
}

