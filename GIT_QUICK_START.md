# 🚀 Philem Android 프로젝트 Git 연결 가이드

## 📌 목표
로컬 Android 프로젝트를 Organization 저장소에 연결하기

**Organization 저장소:**
```
https://github.com/CAU-Philem/philem-python-backend.git
```

---

## ⚡ 빠른 시작 (3단계)

### 1️⃣ PowerShell 또는 Terminal 열기

Android Studio 하단의 **Terminal** 탭 클릭

### 2️⃣ 다음 명령어 실행

```powershell
# 프로젝트 폴더로 이동 (이미 Terminal에 있다면 Skip)
cd C:\Users\yjh2317\AndroidStudioProjects\Philem

# Git 초기화 (처음이라면)
git init

# 원격 저장소 확인 (기존에 설정된 게 있는지)
git remote -v
```

### 3️⃣ Organization 저장소 연결

#### A. 기존 origin이 **없는 경우:**
```powershell
git remote add origin https://github.com/CAU-Philem/philem-python-backend.git
```

#### B. 기존 origin이 **있는 경우:**
```powershell
# 기존 origin 제거
git remote remove origin

# 새 origin 추가
git remote add origin https://github.com/CAU-Philem/philem-python-backend.git
```

### 4️⃣ 확인
```powershell
git remote -v
```

**결과가 다음과 같으면 성공:**
```
origin  https://github.com/CAU-Philem/philem-python-backend.git (fetch)
origin  https://github.com/CAU-Philem/philem-python-backend.git (push)
```

---

## 📤 첫 번째 푸시

### 1. 모든 파일 추가 및 커밋

```powershell
# 현재 상태 확인
git status

# 모든 파일 추가
git add .

# 커밋
git commit -m "feat: Android 앱 초기 구현 - 가격 분석 기능"
```

### 2. 메인 브랜치 설정 및 푸시

```powershell
# 브랜치를 main으로 설정
git branch -M main

# Organization 저장소로 푸시
git push -u origin main
```

---

## 🔐 인증 (처음 푸시 시)

푸시할 때 인증을 요구하면:

### 방법 1: Personal Access Token (권장)

1. **GitHub 웹사이트 접속**
2. **Settings → Developer settings → Personal access tokens → Tokens (classic)**
3. **"Generate new token (classic)"** 클릭
4. **권한 선택:**
   - ✅ `repo` (전체 체크)
   - ✅ `workflow` (선택)
5. **"Generate token"** 클릭
6. **토큰 복사** (⚠️ 한 번만 볼 수 있음!)

7. **푸시 시 입력:**
   - Username: `GitHub 사용자명`
   - Password: `복사한 토큰` 붙여넣기

### 방법 2: Git Credential Manager

Windows에서는 Git Credential Manager가 자동으로 설치되어 있습니다.
첫 푸시 시 로그인 창이 뜨면 GitHub 계정으로 로그인하면 됩니다.

---

## 🌿 브랜치 전략 (선택사항)

### Android 앱용 브랜치 생성

백엔드와 프론트엔드를 구분하려면:

```powershell
# android 브랜치 생성 및 전환
git checkout -b android

# 푸시
git push -u origin android
```

---

## 📂 .gitignore 확인

Organization에 푸시하기 전에 `.gitignore`가 제대로 설정되어 있는지 확인:

```powershell
# .gitignore 파일 확인
cat .gitignore
```

**포함되어야 할 내용:**
```gitignore
# Android
*.iml
.gradle
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx

# Keystore (중요!)
*.jks
*.keystore

# API Keys (중요!)
local.properties
```

---

## 🚨 문제 해결

### 1. "fatal: remote origin already exists"
```powershell
git remote remove origin
git remote add origin https://github.com/CAU-Philem/philem-python-backend.git
```

### 2. "fatal: not a git repository"
```powershell
git init
git remote add origin https://github.com/CAU-Philem/philem-python-backend.git
```

### 3. "Updates were rejected..."
```powershell
# 원격 저장소에 파일이 이미 있는 경우
git pull origin main --allow-unrelated-histories

# 그 후 푸시
git push -u origin main
```

### 4. "Permission denied" 또는 "403 Forbidden"
- Organization 멤버로 초대되었는지 확인
- Personal Access Token 권한 확인 (`repo` 체크)
- GitHub 계정 로그인 상태 확인

---

## 👥 Organization 권한 확인

Organization 관리자에게 확인:

1. ✅ Organization 멤버로 초대되었는지
2. ✅ Repository 접근 권한이 있는지 (Write 권한)
3. ✅ 이메일 초대를 수락했는지

**확인 방법:**
- GitHub → CAU-Philem Organization 페이지 접속
- philem-python-backend 저장소가 보이는지 확인

---

## 📋 체크리스트

- [ ] Terminal/PowerShell 열기
- [ ] `git init` 실행 (처음이라면)
- [ ] `git remote add origin https://github.com/CAU-Philem/philem-python-backend.git`
- [ ] `git remote -v`로 확인
- [ ] `.gitignore` 확인
- [ ] `git add .`
- [ ] `git commit -m "feat: Android 앱 초기 구현"`
- [ ] `git branch -M main`
- [ ] `git push -u origin main`
- [ ] GitHub 웹에서 파일 업로드 확인

---

## 🎯 완료 후

성공적으로 푸시되면:

1. **GitHub 웹사이트 확인**
   ```
   https://github.com/CAU-Philem/philem-python-backend
   ```

2. **README 추가 (선택)**
   ```powershell
   # Android 프로젝트 설명을 위한 README
   echo "# Philem Android App" > README.md
   git add README.md
   git commit -m "docs: Add README"
   git push
   ```

---

## 🔄 이후 작업 흐름

### 일반적인 Git 작업:

```powershell
# 1. 코드 수정 후

# 2. 상태 확인
git status

# 3. 변경사항 추가
git add .

# 4. 커밋
git commit -m "feat: 검색 화면 UI 개선"

# 5. 푸시
git push
```

---

## 💡 팁

### Android Studio에서 Git 사용

1. **VCS 메뉴 활성화:**
   - VCS → Enable Version Control Integration → Git

2. **Commit 창:**
   - Ctrl+K (커밋)
   - Ctrl+Shift+K (푸시)

3. **Git 로그 보기:**
   - Alt+9 (Git 탭)

---

<div align="center">

## 🎉 준비 완료!

이제 Organization 저장소에 연결되었습니다.

**저장소 URL:**
```
https://github.com/CAU-Philem/philem-python-backend
```

**팀원과 협업을 시작하세요!**

</div>

