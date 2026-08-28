@echo off
REM ============================================
REM ���������¹��� �� Docker ���񷢲��ű���һ������ + ���ͣ�
REM
REM �÷�: scripts\publish-docker.bat [�汾��]     Ĭ�ϰ汾�� v2.2
REM ǰ��: 1) �� docker login���� Read&Write Ȩ�޵� token��ֻ�� token ���ͻᱻ�ܣ�
REM       2) �ɷ��� Docker Hub�������������� Docker Desktop ���ô�����
REM Ч��: ���¹��� backend/frontend �� �� tag �� ���� Docker Hub �� ͬ�� docker-compose.yml �汾��
REM ============================================
setlocal enabledelayedexpansion

set "VERSION=%1"
if "%VERSION%"=="" set "VERSION=v2.2"

echo ============================================
echo  [1/5] ���¹������񣨵�ǰ�汾 %VERSION%��
echo ============================================
docker build -t jerryvon/opc-backend:%VERSION% .
if errorlevel 1 goto :fail
docker build -t jerryvon/opc-frontend:%VERSION% -f ruoyi-ui/Dockerfile .
if errorlevel 1 goto :fail

echo ============================================
echo  [2/5] ���ͺ�˾��� jerryvon/opc-backend:%VERSION%
echo ============================================
docker push jerryvon/opc-backend:%VERSION%
if errorlevel 1 goto :fail

echo ============================================
echo  [3/5] ����ǰ�˾��� jerryvon/opc-frontend:%VERSION%
echo ============================================
docker push jerryvon/opc-frontend:%VERSION%
if errorlevel 1 goto :fail

echo ============================================
echo  [4/5] ͬ�� docker-compose.yml �汾��
echo ============================================
node -e "const fs=require('fs');const f='docker-compose.yml';const s=fs.readFileSync(f,'utf8').replace(/opc-backend:[a-zA-Z0-9._-]+/g,'opc-backend:%VERSION%').replace(/opc-frontend:[a-zA-Z0-9._-]+/g,'opc-frontend:%VERSION%');fs.writeFileSync(f,s);console.log('docker-compose.yml �Ѹ���Ϊ %VERSION%');"
if errorlevel 1 goto :fail

echo ============================================
echo  [5/5] ��֤Զ�̾������
echo ============================================
powershell -Command "try { $r=Invoke-RestMethod -Uri 'https://hub.docker.com/v2/repositories/jerryvon/opc-backend/tags' -Headers @{'User-Agent'='Mozilla/5.0'} -ErrorAction Stop; $t=$r.results | Where-Object { $_.name -eq '%VERSION%' }; if($t){ Write-Host '[OK] ��˾������� Docker Hub: jerryvon/opc-backend:%VERSION%' } else { Write-Host '[WARN] ��˾���δ�鵽���Ժ�ˢ��ҳ��ȷ�ϣ�' } } catch { Write-Host '[WARN] ��֤�ӿڲ��ɴ������������ͳɹ���ҳ��ɲ飩' }"

echo.
echo [OK] ������� %VERSION%
echo    �ֿ�ҳ: https://hub.docker.com/u/jerryvon
echo    ע��: docker-compose.yml ��ͬ���汾�ţ��ǵ� git commit �浵
exit /b 0

:fail
echo.
echo [FAIL] ����ʧ�ܣ����Ϸ�������Ϣ��
exit /b 1
