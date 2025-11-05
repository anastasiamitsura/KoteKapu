# app/__init__.py
import os
from datetime import timedelta
from flask import Flask, jsonify, request
from flask_cors import CORS
from .extensions import db, bcrypt, jwt  # не импортируем routes здесь

def create_app():
    app = Flask(__name__)
    CORS(app)

    # Конфигурация
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///app_new.db'
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-fallback-key')
    app.config['JWT_SECRET_KEY'] = os.environ.get('JWT_SECRET_KEY', 'dev-jwt-fallback')
    app.config['JWT_ACCESS_TOKEN_EXPIRES'] = timedelta(days=7)

    # инициализация расширений
    db.init_app(app)
    bcrypt.init_app(app)
    jwt.init_app(app)

    # Импортируем и регистрируем blueprint здесь — чтобы избежать circular import
    from .routes import bp as api_bp  # локальный импорт
    app.register_blueprint(api_bp, url_prefix='')



    # Импорт моделей (если нужно их инициализировать/зарегистрировать метаданные)
    try:
        from . import models  # noqa: F401
    except Exception:
        # если что-то ломается при импорте моделей — логируем/игнорируем, но не мешаем стартапу приложения
        pass


    return app
