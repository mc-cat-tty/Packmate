# Packmate
## Клонирование
Поскольку этот репозиторий содержит фронтенд как submodule, его необходимо клонировать так:
```bash
git clone --recurse-submodules https://gitlab.com/binarybears_ctf/Packmate.git

# Или, на старых версиях git
git clone --recursive https://gitlab.com/binarybears_ctf/Packmate.git
```

Если репозиторий уже был склонирован без подмодулей, необходимо выполнить:
```bash
git pull  # Забираем свежую версию мастер-репы из gitlab
git submodule update --init --recursive
```

## Сборка
В этом ПО используется Docker и docker-compose. В образ `packmate-app` пробрасывается сетевой интерфейс хоста, его название автоматически определяется в `./start.sh`.

Так как невозможно использовать `links:` вместе с `network_mode: "host"` в docker-compose, `packmate-db` настроен на прослушивание порта 65001 с локальным ip.

Для сборки и запуска:
```bash
export PACKMATE_LOCAL_IP='192.168...'  # IP хоста в перехватываемой сети
export PACKMATE_INTERFACE='eth0'  # Сетевой интерфейс для перехвата пакетов

# Дальше все экспорты опциональны
export PACKMATE_WEB_LOGIN='BinaryBears'  # Имя пользователя для веб-интерфейса
export PACKMATE_WEB_PASSWORD='123456'  # Пароль для веб-интерфейса

docker-compose up --build
```

После успешного запуска Packmate будет видно с любого хоста на порту `65000`.
