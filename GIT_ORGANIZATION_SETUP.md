# 🔄 Organization 저장소로 연결하기

## 📌 상황
- 현재: 로컬 프로젝트가 있음
- 목표: GitHub Organization의 새 저장소로 연결

---

## 🚀 단계별 가이드

### 1단계: Organization에 새 저장소 생성

1. GitHub 웹사이트 접속
2. Organization 페이지로 이동
3. **"New repository"** 클릭
4. 저장소 이름 입력 (예: `Philem`)
5. **"Create repository"** 클릭

> ⚠️ **주의**: "Initialize this repository with a README" 체크 **안 함**

---

### 2단계: 로컬에서 Git 초기화 (이미 되어있다면 Skip)

```powershell
cd C:\Users\yjh2317\AndroidStudioProjects\Philem
git init
```

---

### 3단계: 현재 원격 저장소 확인

```powershell
git remote -v
```

**결과 예시:**
```
origin  https://github.com/your-username/Philem.git (fetch)
origin  https://github.com/your-username/Philem.git (push)
```

---

### 4단계: 기존 원격 저장소 제거 (있다면)

```powershell
git remote remove origin
```

---

### 5단계: Organization 저장소 연결

**Organization 저장소 URL을 origin으로 추가:**

```powershell
git remote add origin https://github.com/[Organization이름]/Philem.git
```

**예시:**
```powershell
git remote add origin https://github.com/MyCompany/Philem.git
```

---

### 6단계: 확인

```powershell
git remote -v
```

**결과:**
```
origin  https://github.com/[Organization이름]/Philem.git (fetch)
origin  https://github.com/[Organization이름]/Philem.git (push)
```

---

### 7단계: 모든 파일 커밋 (처음이라면)

```powershell
# 모든 파일 추가
git add .

# 커밋
git commit -m "Initial commit: Philem 중고 카메라 가격 분석 앱"
```

---

### 8단계: Organization 저장소로 푸시

```powershell
# 메인 브랜치로 푸시
git push -u origin main
```

또는 (브랜치가 master인 경우):
```powershell
git push -u origin master
```

---

## 🔐 인증 방법

### SSH 사용 (권장)

**1. SSH 키 생성 (없다면):**
```powershell
ssh-keygen -t ed25519 -C "your_email@example.com"
```

**2. SSH 키 복사:**
```powershell
cat ~/.ssh/id_ed25519.pub
```

**3. GitHub에 SSH 키 등록:**
- GitHub Settings → SSH and GPG keys → New SSH key
- 복사한 키 붙여넣기

**4. Organization 저장소를 SSH로 연결:**
```powershell
git remote set-url origin git@github.com:[Organization이름]/Philem.git
```

---

### Personal Access Token 사용

**1. GitHub에서 토큰 생성:**
- Settings → Developer settings → Personal access tokens → Tokens (classic)
- Generate new token (classic)
- 권한 선택: `repo` 전체 체크
- Generate token
- **토큰 복사 (다시 볼 수 없음!)**

**2. 푸시 시 토큰 사용:**
```powershell
git push -u origin main
```
- Username: GitHub 사용자명
- Password: **생성한 토큰** 붙여넣기

---

## 🌿 브랜치 관리

### 현재 브랜치 확인
```powershell
git branch
```

### 브랜치 이름 변경 (master → main)
```powershell
git branch -M main
```

### 새 브랜치 생성 및 전환
```powershell
git checkout -b develop
git push -u origin develop
```

---

## 🔄 Organization 저장소 클론 (팀원용)

팀원이 Organization 저장소를 클론하는 방법:

```powershell
git clone https://github.com/[Organization이름]/Philem.git
cd Philem
```

---

## ⚙️ Android Studio에서 설정

### 방법 1: Android Studio UI 사용

1. **VCS → Git → Remotes...**
2. **"+" 버튼** 클릭
3. **Name**: `origin`
4. **URL**: `https://github.com/[Organization이름]/Philem.git`
5. **OK** 클릭

### 방법 2: Terminal 탭 사용

Android Studio 하단의 **Terminal** 탭에서:

```powershell
git remote set-url origin https://github.com/[Organization이름]/Philem.git
git push -u origin main
```

---

## 📝 .gitignore 확인

Organization에 푸시하기 전에 `.gitignore` 파일 확인:

```gitignore
# Android
*.iml
.gradle
/local.properties
/.idea/workspace.xml
/.idea/navEditor.xml
/.idea/assetWizardSettings.xml
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties

# Keystore files
*.jks
*.keystore

# API Keys
local.properties
```

---

## 🎯 빠른 참조

### 상황별 명령어

| 상황 | 명령어 |
|------|--------|
| 원격 저장소 확인 | `git remote -v` |
| 원격 저장소 제거 | `git remote remove origin` |
| 원격 저장소 추가 | `git remote add origin [URL]` |
| 원격 저장소 URL 변경 | `git remote set-url origin [URL]` |
| 푸시 | `git push -u origin main` |
| 풀 | `git pull origin main` |
| 상태 확인 | `git status` |

---

## 🚨 문제 해결

### 1. "fatal: remote origin already exists"
```powershell
git remote remove origin
git remote add origin [새 URL]
```

### 2. "Updates were rejected because the tip of your current branch is behind"
```powershell
# 강제 푸시 (주의!)
git push -f origin main

# 또는 풀 후 푸시
git pull origin main --rebase
git push origin main
```

### 3. "Permission denied (publickey)"
```powershell
# SSH 키 확인
ssh -T git@github.com

# SSH 키 재설정
ssh-keygen -t ed25519 -C "your_email@example.com"
# GitHub에 새 키 등록
```

### 4. "Authentication failed"
- Personal Access Token 재생성
- 토큰 권한 확인 (`repo` 권한 필요)

---

## 📂 Organization 권한 설정

Organization 관리자가 해야 할 일:

1. **Settings → Member privileges**
2. **Base permissions**: Read 또는 Write
3. **팀원 초대**: People → Invite member
4. **저장소 권한**: Repository → Settings → Manage access

---

## ✅ 체크리스트

- [ ] Organization에 새 저장소 생성
- [ ] 로컬에서 `git remote -v` 확인
- [ ] 기존 origin 제거 (있다면)
- [ ] Organization 저장소 URL 추가
- [ ] `.gitignore` 파일 확인
- [ ] 모든 변경사항 커밋
- [ ] `git push -u origin main` 실행
- [ ] GitHub 웹에서 파일 업로드 확인
- [ ] 팀원에게 저장소 URL 공유

---

## 🎉 완료!

이제 Organization 저장소에 연결되었습니다!

**Organization 저장소 URL:**
```
https://github.com/[Organization이름]/Philem
```

팀원들은 이 URL로 클론할 수 있습니다.

---

<div align="center">

**Made with 💙 for Philem Team**

</div>

