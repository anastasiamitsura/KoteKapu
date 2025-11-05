
import sys

from flask import jsonify

from app import create_app
app = create_app()


if __name__ == '__main__':


    if '--init-db' in sys.argv:
        from app.init_db import init_db
        print('Starting DB initialization...')
        init_db(app)
    else:
        print("🚀 ЗАПУСКАЕМ СЕРВЕР...")
        print("📊 Используем SQLite (app_new.db)")
        print("🌐 Сервер доступен по: http://localhost:5000")
        print("📝 ОСНОВНЫЕ НОВЫЕ МАРШРУТЫ:")
        print("   POST /api/users/<id>/complete-profile - заполнение профиля")
        print("   POST /api/users/<id>/complete-preferences - опрос предпочтений")
        print("   GET  /api/preferences/categories - категории для опроса")
        print("   POST /api/events/<id>/register - регистрация на событие")
        print("   POST /api/organisations - создание организации")
        print("   POST /api/organisations/<id>/subscribe - подписка на организацию")
        app.run(host='0.0.0.0', port=5000, debug=True)
