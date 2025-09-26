# AWS OIDC Setup for GitHub Actions

## 1. AWS IAM OIDC Identity Provider 설정

### AWS 콘솔에서:
1. IAM → Identity providers → Add provider
2. Provider type: OpenID Connect
3. Provider URL: `https://token.actions.githubusercontent.com`
4. Audience: `sts.amazonaws.com`
5. Add provider 클릭

## 2. IAM Role 생성

### AWS 콘솔에서:
1. IAM → Roles → Create role
2. Trusted entity type: Web identity
3. Identity provider: `token.actions.githubusercontent.com`
4. Audience: `sts.amazonaws.com`
5. GitHub organization: `prgrms-aibe-devcourse`
6. GitHub repository: `AIBE2_FinalProject_Compass_BE`
7. GitHub branch (선택사항): `main`, `develop`, `test/*`

### Trust Policy 편집:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:aws:iam::YOUR_ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
                },
                "StringLike": {
                    "token.actions.githubusercontent.com:sub": "repo:prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE:*"
                }
            }
        }
    ]
}
```

### 필요한 권한 정책 연결:
- `AWSElasticBeanstalkWebTier`
- `AWSElasticBeanstalkWorkerTier`
- `AWSElasticBeanstalkMulticontainerDocker`
- S3 버킷 접근 권한 (커스텀 정책)

### S3 버킷 정책 예시:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:PutObjectAcl",
                "s3:GetObject",
                "s3:GetObjectAcl",
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::YOUR-S3-BUCKET-NAME/*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket"
            ],
            "Resource": "arn:aws:s3:::YOUR-S3-BUCKET-NAME"
        }
    ]
}
```

## 3. GitHub Repository Secrets 설정

GitHub Repository → Settings → Secrets and variables → Actions에서 다음 시크릿 추가:

| Secret Name | Description | Example Value |
|-------------|-------------|---------------|
| `AWS_ROLE_ARN` | IAM Role ARN | `arn:aws:iam::123456789012:role/GitHubActions-ElasticBeanstalk-Role` |
| `S3_BUCKET_NAME` | S3 버킷 이름 | `compass-eb-deployments` |
| `EB_APPLICATION_NAME` | Elastic Beanstalk 앱 이름 | `compass-backend` |
| `EB_ENVIRONMENT_NAME` | Elastic Beanstalk 환경 이름 | `compass-backend-env` |

## 4. S3 버킷 생성 (없는 경우)

```bash
aws s3 mb s3://compass-eb-deployments --region ap-northeast-2
```

## 5. Elastic Beanstalk Application 생성 (없는 경우)

```bash
# Application 생성
aws elasticbeanstalk create-application \
  --application-name compass-backend \
  --description "Compass Backend Application" \
  --region ap-northeast-2

# Environment 생성
aws elasticbeanstalk create-environment \
  --application-name compass-backend \
  --environment-name compass-backend-env \
  --solution-stack-name "64bit Amazon Linux 2023 v4.2.0 running Corretto 17" \
  --region ap-northeast-2
```

## 6. 환경 변수 설정 (Elastic Beanstalk 콘솔에서)

Elastic Beanstalk → Environments → compass-backend-env → Configuration → Software → Environment properties:

| Name | Value |
|------|-------|
| `DATABASE_URL` | `jdbc:postgresql://compass-db.xxxx.rds.amazonaws.com:5432/compass` |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | `***` |
| `JWT_SECRET` | `***` |
| `JWT_ACCESS_SECRET` | `***` |
| `JWT_REFRESH_SECRET` | `***` |
| `GOOGLE_CLOUD_PROJECT_ID` | `travelagent-xxxxx` |
| `GOOGLE_CLOUD_LOCATION` | `us-central1` |
| `GEMINI_MODEL` | `gemini-2.0-flash` |
| `AWS_S3_BUCKET_NAME` | `compass-travel-images` |
| `AWS_S3_REGION` | `ap-northeast-2` |

## 7. 문제 해결

### "Request ARN is invalid" 에러 해결:
1. IAM Role ARN이 올바른지 확인
2. Role의 Trust Policy가 올바른지 확인
3. GitHub organization과 repository 이름이 정확한지 확인
4. `token.actions.githubusercontent.com:sub` 조건이 올바른지 확인

### 예시 Role ARN 형식:
```
arn:aws:iam::123456789012:role/GitHubActions-ElasticBeanstalk-Role
```

## 8. 테스트

1. 변경사항을 커밋하고 푸시
2. GitHub Actions 탭에서 워크플로우 실행 확인
3. AWS Elastic Beanstalk 콘솔에서 배포 상태 확인