# 이 프로젝트의 컨테이너만 묶어서 관리합니다. (.env 의 COMPOSE_PROJECT_NAME 기준)
COMPOSE = docker compose

.PHONY: up down restart logs ps db clean

up:        ## 전체 스택 빌드 후 백그라운드 실행
	$(COMPOSE) up -d --build

down:      ## 전체 스택 중지 및 컨테이너·네트워크 제거 (DB 데이터 유지)
	$(COMPOSE) down

restart: down up

logs:      ## 전체 로그 따라가기
	$(COMPOSE) logs -f

ps:        ## 이 프로젝트 컨테이너 상태
	$(COMPOSE) ps

db:        ## 로컬 개발용으로 DB 만 실행
	$(COMPOSE) up -d db

clean:     ## 중지 + DB 볼륨과 이미지까지 삭제 (데이터 초기화)
	$(COMPOSE) down -v --rmi local
