#!/usr/bin/env python3
"""
Запуск сервера с тестовыми эндпоинтами
"""

from app import app
from test_endpoints import *  # Импортируем тестовые эндпоинты

if __name__ == '__main__':
    print("🚀 ЗАПУСКАЕМ ТЕСТОВЫЙ СЕРВЕР...")
    print("📊 Используем SQLite (app_new.db)")
    print("🌐 Сервер доступен по: http://localhost:5000")
    print("🧪 ТЕСТОВЫЕ ЭНДПОИНТЫ:")
    print("   GET /api/test/connection - проверка связи")
    print("   GET /api/test/simple-feed - упрощенная лента")
    print("==========================================")
    app.run(host='0.0.0.0', port=5000, debug=True)