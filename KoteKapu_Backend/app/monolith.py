
# Original monolithic application content retained here as reference.
# For maintainability, main app logic was moved into modular files.
# You may inspect the original code below if needed.
_original_source = r"""
# This file was auto-generated from original app.py
from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_bcrypt import Bcrypt
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity
from flask_cors import CORS
from datetime import datetime, timedelta
import json
import os
import random
import re

app = Flask(__name__)
CORS(app)

# Конфигурация
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///app_new.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-fallback-key')
app.config['JWT_SECRET_KEY'] = os.environ.get('JWT_SECRET_KEY', 'dev-jwt-fallback')
app.config['JWT_ACCESS_TOKEN_EXPIRES'] = timedelta(days=7)

db = SQLAlchemy(app)
bcrypt = Bcrypt(app)
jwt = JWTManager(app)

# Константы для опроса предпочтений
INTEREST_CATEGORIES = [
    "Технологии и Инновации", "Искусство и Культура", "Наука и Просвещение",
    "Карьера и Бизнес", "Здоровье и Спорт", "Волонтерство и Благотворительность",
    "Языки и Путешествия", "Гейминг и Киберспорт", "Медиа и Блогинг", "Общество и Урбанистика"
]

EVENT_TYPES = [
    "хакатон", "лекция", "мастер-класс", "концерт", "встреча", "семинар",
    "воркшоп", "конференция", "выставка", "фестиваль", "конкурс", "чемпионат"
]

FORMAT_TYPES = ["онлайн", "офлайн", "гибрид"]


# Модель достижений
class Achievement(db.Model):
    __tablename__ = 'achievement'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    description = db.Column(db.String(500))
    points = db.Column(db.Integer, default=0)


# Модель организации
class Organisation(db.Model):
    __tablename__ = 'organisation'
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(250), unique=True, nullable=False)
    description = db.Column(db.String(1000), nullable=False)
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    avatar = db.Column(db.String(500), nullable=True)
    owner_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    status = db.Column(db.String(20), default='pending')  # pending, approved, rejected
    city = db.Column(db.String(100), nullable=True)
    social_links = db.Column(db.Text, default='[]')
    tags = db.Column(db.Text, default='[]')


# Модель пользователя
class User(db.Model):
    __tablename__ = 'user'
    id = db.Column(db.Integer, primary_key=True)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    first_name = db.Column(db.String(50), nullable=False)
    last_name = db.Column(db.String(50), nullable=False)
    created_at = db.Column(db.DateTime, server_default=db.func.now())

    # Основная информация (Сценарий 2)
    phone = db.Column(db.String(20), nullable=True)
    age_user = db.Column(db.Integer, nullable=True)
    placement = db.Column(db.String(100), nullable=True)
    study_place = db.Column(db.String(100), nullable=True)
    grade_course = db.Column(db.String(50), nullable=True)  # класс/курс
    exp = db.Column(db.Integer, default=0)
    avatar = db.Column(db.String(500), nullable=True)

    # Статус заполнения профиля
    profile_completed = db.Column(db.Boolean, default=False)
    preferences_completed = db.Column(db.Boolean, default=False)

    # Метрики для ленты рекомендаций
    interests_metrics = db.Column(db.Text, default=json.dumps({
        'IT': 0.1, 'искусства': 0.1, 'музыка': 0.1, 'языки': 0.1,
        'экономика': 0.1, 'менеджмент': 0.1, 'творчество': 0.1,
        'спорт': 0.1, 'инжинерия': 0.1, 'культура': 0.1
    }))
    format_metrics = db.Column(db.Text, default=json.dumps({
        'онлайн': 0.33, 'офлайн': 0.33, 'гибрид': 0.34
    }))
    event_type_metrics = db.Column(db.Text, default=json.dumps({}))

    # Метрики ленты рекомендаций
    feed_metrics = db.Column(db.Text, default=json.dumps({
        'click_rate': 0.0,
        'like_rate': 0.0,
        'time_spent': 0.0,
        'completion_rate': 0.0,
        'preferred_categories': {},
        'preferred_formats': {},
        'preferred_event_types': {}
    }))


    def set_password(self, password):
        self.password_hash = bcrypt.generate_password_hash(password).decode('utf-8')

    def check_password(self, password):
        return bcrypt.check_password_hash(self.password_hash, password)

    def get_interests_metrics(self):
        return json.loads(self.interests_metrics)

    def set_interests_metrics(self, metrics_dict):
        self.interests_metrics = json.dumps(metrics_dict)

    def get_format_metrics(self):
        return json.loads(self.format_metrics)

    def set_format_metrics(self, metrics_dict):
        self.format_metrics = json.dumps(metrics_dict)

    def get_event_type_metrics(self):
        return json.loads(self.event_type_metrics)

    def set_event_type_metrics(self, metrics_dict):
        self.event_type_metrics = json.dumps(metrics_dict)

    def get_feed_metrics(self):
        return json.loads(self.feed_metrics)

    def set_feed_metrics(self, metrics_dict):
        self.feed_metrics = json.dumps(metrics_dict)

    def update_feed_metrics(self, post, action_type, value=1.0):
        metrics = self.get_feed_metrics()
        post_tags = post.get_tags()

        if action_type == 'click':
            metrics['click_rate'] = (metrics['click_rate'] + value) / 2
        elif action_type == 'like':
            metrics['like_rate'] = (metrics['like_rate'] + value) / 2

            # Обновляем предпочтения по категориям
            for tag in post_tags['interests']:
                current_value = metrics['preferred_categories'].get(tag, 0)
                metrics['preferred_categories'][tag] = current_value + 0.1

            # Обновляем предпочтения по форматам
            for tag in post_tags['formats']:
                current_value = metrics['preferred_formats'].get(tag, 0)
                metrics['preferred_formats'][tag] = current_value + 0.1

        # Нормализуем предпочтения
        metrics['preferred_categories'] = normalize_metrics(metrics['preferred_categories'])
        metrics['preferred_formats'] = normalize_metrics(metrics['preferred_formats'])
        metrics['preferred_event_types'] = normalize_metrics(metrics['preferred_event_types'])

        self.set_feed_metrics(metrics)

    def to_dict(self):
        return {
            'id': self.id,
            'email': self.email,
            'first_name': self.first_name,
            'last_name': self.last_name,
            'phone': self.phone,
            'age_user': self.age_user,
            'placement': self.placement,
            'study_place': self.study_place,
            'grade_course': self.grade_course,
            'exp': self.exp,
            'avatar': self.avatar,
            'profile_completed': bool(self.profile_completed),
            'preferences_completed': bool(self.preferences_completed),
            'interests_metrics': self.get_interests_metrics(),
            'format_metrics': self.get_format_metrics(),
            'event_type_metrics': self.get_event_type_metrics(),
            'feed_metrics': self.get_feed_metrics(),
            'created_at': self.created_at.isoformat() if self.created_at else None
        }


class PostEvent(db.Model):
    __tablename__ = 'post_event'
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(250), nullable=False)
    description = db.Column(db.String(1000), nullable=False)
    date_time = db.Column(db.DateTime, nullable=False)
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    pic = db.Column(db.String(500), nullable=True)
    location = db.Column(db.String(500), nullable=True)  # для офлайн мероприятий
    event_type = db.Column(db.String(100), nullable=True)  # тип события

    # Теги для рекомендаций
    interest_tags = db.Column(db.Text, default='[]')
    format_tags = db.Column(db.Text, default='[]')

    # Внешние ключи
    organization_id = db.Column(db.Integer, db.ForeignKey('organisation.id'), nullable=False)

    def set_interest_tags(self, tags_list):
        self.interest_tags = json.dumps(tags_list)

    def get_interest_tags(self):
        return json.loads(self.interest_tags)

    def set_format_tags(self, tags_list):
        self.format_tags = json.dumps(tags_list)

    def get_format_tags(self):
        return json.loads(self.format_tags)

    def get_tags(self):
        return {
            'interests': self.get_interest_tags(),
            'formats': self.get_format_tags(),
            'event_type': self.event_type
        }

    def calculate_relevance_score(self, user):
        try:
            user_interests = user.get_interests_metrics()
            user_formats = user.get_format_metrics()
            user_event_types = user.get_event_type_metrics()
            user_feed_metrics = user.get_feed_metrics()

            post_interests = self.get_interest_tags()
            post_formats = self.get_format_tags()

            interest_score = sum(user_interests.get(tag, 0.0) for tag in post_interests)
            format_score = sum(user_formats.get(tag, 0.0) for tag in post_formats)
            event_type_score = user_event_types.get(self.event_type, 0.0) if self.event_type else 0.0

            feed_interest_score = sum(user_feed_metrics['preferred_categories'].get(tag, 0) for tag in post_interests)
            feed_format_score = sum(user_feed_metrics['preferred_formats'].get(tag, 0) for tag in post_formats)
            feed_event_score = user_feed_metrics['preferred_event_types'].get(self.event_type,
                                                                              0) if self.event_type else 0.0

            total_score = (
                    interest_score * 0.3 +
                    format_score * 0.25 +
                    event_type_score * 0.2 +
                    feed_interest_score * 0.1 +
                    feed_format_score * 0.1 +
                    feed_event_score * 0.05
            )

            return total_score

        except Exception as e:
            print(f"ERROR: Ошибка в calculate_relevance_score: {e}")
            return 0.1

    def to_dict(self):
        try:
            org = Organisation.query.get(self.organization_id)
            org_data = None
            if org:
                org_data = {
                    'id': org.id,
                    'title': org.title,
                    'avatar': org.avatar
                }

            # Безопасно получаем количество лайков и регистраций
            likes_count = 0
            registered_count = 0

            if hasattr(self, 'liked_by'):
                likes_count = len(self.liked_by)

            if hasattr(self, 'registered_users'):
                registered_count = len(self.registered_users)

            return {
                'id': self.id,
                'title': self.title,
                'description': self.description,
                'date_time': self.date_time.isoformat() if self.date_time else None,
                'created_at': self.created_at.isoformat() if self.created_at else None,
                'pic': self.pic,
                'location': self.location,
                'event_type': self.event_type,
                'interest_tags': self.get_interest_tags(),
                'format_tags': self.get_format_tags(),
                'organization_id': self.organization_id,
                'organization_name': org.title if org else None,
                'organization_avatar': org.avatar if org else None,
                'type': 'event',
                'likes': likes_count,
                'registered_count': registered_count
            }
        except Exception as e:
            print(f"ERROR in PostEvent.to_dict(): {e}")
            return {
                'id': self.id,
                'title': self.title,
                'description': 'Ошибка загрузки данных',
                'type': 'event'
            }


class PostSimple(db.Model):
    __tablename__ = 'post_simple'
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(250), nullable=False)
    description = db.Column(db.String(1000), nullable=False)
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    pic = db.Column(db.String(500), nullable=True)

    # Теги для рекомендаций
    interest_tags = db.Column(db.Text, default='[]')
    format_tags = db.Column(db.Text, default='[]')

    # Внешние ключи
    author_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=True)
    organization_id = db.Column(db.Integer, db.ForeignKey('organisation.id'), nullable=True)

    def set_interest_tags(self, tags_list):
        self.interest_tags = json.dumps(tags_list)

    def get_interest_tags(self):
        return json.loads(self.interest_tags)

    def set_format_tags(self, tags_list):
        self.format_tags = json.dumps(tags_list)

    def get_format_tags(self):
        return json.loads(self.format_tags)

    def get_tags(self):
        return {
            'interests': self.get_interest_tags(),
            'formats': self.get_format_tags()
        }

    def calculate_relevance_score(self, user):
        try:
            # Получаем метрики пользователя
            user_interests = user.get_interests_metrics() or {}
            user_formats = user.get_format_metrics() or {}
            user_feed_metrics = user.get_feed_metrics() or {}

            # Теги поста
            post_interests = self.get_interest_tags() or []
            post_formats = self.get_format_tags() or []

            # Расчет оценки по интересам
            interest_score = 0.0
            for tag in post_interests:
                interest_score += user_interests.get(tag, 0.0)

            # Нормализуем оценку интересов
            if post_interests:
                interest_score = interest_score / len(post_interests)

            # Расчет оценки по форматам
            format_score = 0.0
            for tag in post_formats:
                format_score += user_formats.get(tag, 0.0)

            if post_formats:
                format_score = format_score / len(post_formats)

            # Учет метрик ленты
            feed_preferred_categories = user_feed_metrics.get('preferred_categories', {})
            feed_preferred_formats = user_feed_metrics.get('preferred_formats', {})

            feed_interest_score = sum(feed_preferred_categories.get(tag, 0) for tag in post_interests)
            feed_format_score = sum(feed_preferred_formats.get(tag, 0) for tag in post_formats)

            if post_interests:
                feed_interest_score = feed_interest_score / len(post_interests)
            if post_formats:
                feed_format_score = feed_format_score / len(post_formats)

            # Итоговая оценка с весами (для постов без event_type)
            total_score = (
                    interest_score * 0.5 +  # Основной вес - интересы из анкеты
                    format_score * 0.3 +  # Форматы из анкеты
                    feed_interest_score * 0.1 +  # Интересы из ленты
                    feed_format_score * 0.1  # Форматы из ленты
            )

            return total_score

        except Exception as e:
            print(f"ERROR: Ошибка в calculate_relevance_score для поста: {e}")
            return 0.1


    def to_dict(self):
        try:
            org_data = None
            author_data = None

            # Получаем организацию если есть
            if self.organization_id:
                org = db.session.get(Organisation, self.organization_id)
                if org:
                    org_data = {
                        'id': org.id,
                        'title': org.title,
                        'avatar': org.avatar
                    }

            # Получаем автора если есть
            if self.author_id:
                author = db.session.get(User, self.author_id)
                if author:
                    author_data = {
                        'id': author.id,
                        'first_name': author.first_name,
                        'last_name': author.last_name,
                        'avatar': author.avatar
                    }

            return {
                'id': self.id,
                'title': self.title,
                'description': self.description,
                'created_at': self.created_at.isoformat() if self.created_at else None,
                'pic': self.pic,
                'interest_tags': self.get_interest_tags(),
                'format_tags': self.get_format_tags(),
                'organization_id': self.organization_id,
                'organization': org_data,
                'author_id': self.author_id,
                'author': author_data,
                'type': 'post'
            }
        except Exception as e:
            print(f"ERROR in PostSimple.to_dict(): {e}")
            return {
                'id': self.id,
                'title': self.title,
                'description': 'Ошибка загрузки данных',
                'type': 'post'
            }


# Вспомогательные таблицы
user_achievements = db.Table('user_achievements',
                             db.Column('user_id', db.Integer, db.ForeignKey('user.id')),
                             db.Column('achievement_id', db.Integer, db.ForeignKey('achievement.id'))
                             )

user_subscriptions = db.Table('user_subscriptions',
                              db.Column('user_id', db.Integer, db.ForeignKey('user.id')),
                              db.Column('organization_id', db.Integer, db.ForeignKey('organisation.id'))
                              )


user_liked_posts = db.Table('user_liked_posts',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id'), primary_key=True),
    db.Column('post_event_id', db.Integer, db.ForeignKey('post_event.id'), primary_key=True)
)

user_registered_events = db.Table('user_registered_events',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id'), primary_key=True),
    db.Column('post_event_id', db.Integer, db.ForeignKey('post_event.id'), primary_key=True)
)

# Relationships
User.achievements = db.relationship('Achievement', secondary=user_achievements, backref='users')
User.subscriptions = db.relationship('Organisation', secondary=user_subscriptions, backref='subscribers')
User.liked_event_posts = db.relationship('PostEvent', secondary=user_liked_posts, backref='liked_by')
User.registered_events = db.relationship('PostEvent', secondary=user_registered_events, backref='registered_users')
User.user_posts = db.relationship('PostSimple', backref='author', lazy=True)
User.user_organisations = db.relationship('Organisation', backref='owner', lazy=True,
                                          foreign_keys='Organisation.owner_id')
Organisation.event_posts = db.relationship('PostEvent', backref='organization', lazy=True)
Organisation.simple_posts = db.relationship('PostSimple', backref='organization', lazy=True)


# Вспомогательные функции
def normalize_metrics(metrics):
    \"\"\"Нормализует метрики так, чтобы сумма была равна 1\"\"\"
    if not metrics:
        return {}
    total = sum(metrics.values())
    if total > 0:
        return {k: v / total for k, v in metrics.items()}
    return metrics


def update_user_interests(user, post):
    \"\"\"Обновляет метрики интересов пользователя на основе лайкнутых постов\"\"\"
    try:
        # Получаем текущие метрики
        interests_metrics = user.get_interests_metrics()
        format_metrics = user.get_format_metrics()
        event_type_metrics = user.get_event_type_metrics()

        # Теги поста
        post_interests = post.get_interest_tags()
        post_formats = post.get_format_tags()
        post_event_type = post.event_type

        # Коэффициент обучения
        learning_rate = 0.1

        # Обновляем интересы
        for tag in post_interests:
            if tag in interests_metrics:
                interests_metrics[tag] = min(1.0, interests_metrics[tag] + learning_rate)
            else:
                interests_metrics[tag] = learning_rate

        # Обновляем форматы
        for tag in post_formats:
            if tag in format_metrics:
                format_metrics[tag] = min(1.0, format_metrics[tag] + learning_rate)
            else:
                format_metrics[tag] = learning_rate

        # Обновляем типы событий
        if post_event_type:
            if post_event_type in event_type_metrics:
                event_type_metrics[post_event_type] = min(1.0, event_type_metrics[post_event_type] + learning_rate)
            else:
                event_type_metrics[post_event_type] = learning_rate

        # Нормализуем метрики
        interests_metrics = normalize_metrics(interests_metrics)
        format_metrics = normalize_metrics(format_metrics)
        event_type_metrics = normalize_metrics(event_type_metrics)

        # Сохраняем обновленные метрики
        user.set_interests_metrics(interests_metrics)
        user.set_format_metrics(format_metrics)
        user.set_event_type_metrics(event_type_metrics)

    except Exception as e:
        print(f"ERROR: Ошибка при обновлении интересов: {e}")


def validate_password(password):
    \"\"\"Валидация пароля\"\"\"
    if len(password) < 6:
        return False, "Пароль должен содержать минимум 6 символов"
    return True, ""


def validate_email(email):
    \"\"\"Валидация email\"\"\"
    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    if not re.match(pattern, email):
        return False, "Неверный формат email"
    return True, ""


import os

# Проверяем существование базы данных
db_file = 'app_new.db'
db_exists = os.path.exists(db_file)

# Создание таблиц и начальных данных
# Создание таблиц и начальных данных
with app.app_context():
    try:
        # Всегда создаем таблицы (если они не существуют)
        db.create_all()
        print("✅ БАЗА ДАННЫХ ГОТОВА!")

        # Проверяем, нужно ли создавать тестовые данные
        existing_users = User.query.count()
        existing_achievements = Achievement.query.count()

        if existing_users == 0 and existing_achievements == 0:
            print("🔄 СОЗДАЕМ ТЕСТОВЫЕ ДАННЫЕ...")

            # Создаем начальные достижения
            initial_achievements = [
                {'name': 'Регистрация на платформе', 'description': 'Вы зарегистрировались на платформе', 'points': 10},
                {'name': 'Первый ивент', 'description': 'Зарегистрировались на первый ивент', 'points': 20},
                {'name': 'Первый лайк', 'description': 'Поставили первый лайк', 'points': 5},
                {'name': 'Первая подписка', 'description': 'Подписались на первую организацию', 'points': 15},
                {'name': 'Первый пост', 'description': 'Создали первый пост', 'points': 25}
            ]

            for ach_data in initial_achievements:
                achievement = Achievement(
                    name=ach_data['name'],
                    description=ach_data['description'],
                    points=ach_data['points']
                )
                db.session.add(achievement)
            print("✅ НАЧАЛЬНЫЕ ДОСТИЖЕНИЯ СОЗДАНЫ!")

            # Создаем тестового пользователя
            user = User(
                email='test_user@example.com',
                first_name='Тест',
                last_name='Пользователь',
                phone='+79991234567',
                age_user=25,
                placement='Москва',
                study_place='МГУ',
                grade_course='3 курс',
                profile_completed=True,
                preferences_completed=True
            )
            user.set_password('password123')

            # Устанавливаем начальные метрики интересов
            initial_interests = {
                'IT': 0.3, 'искусства': 0.1, 'музыка': 0.05, 'языки': 0.05,
                'экономика': 0.1, 'менеджмент': 0.1, 'творчество': 0.1,
                'спорт': 0.05, 'инжинерия': 0.1, 'культура': 0.05
            }
            user.set_interests_metrics(initial_interests)

            initial_formats = {'онлайн': 0.4, 'офлайн': 0.4, 'гибрид': 0.2}
            user.set_format_metrics(initial_formats)

            initial_event_types = {'хакатон': 0.3, 'лекция': 0.2, 'мастер-класс': 0.2, 'встреча': 0.1, 'семинар': 0.2}
            user.set_event_type_metrics(initial_event_types)

            db.session.add(user)
            db.session.commit()
            print("✅ ТЕСТОВЫЙ ПОЛЬЗОВАТЕЛЬ СОЗДАН!")
            user_id = user.id

            # Создаем тестовые организации
            orgs_data = [
                {
                    'title': 'IT Community Moscow',
                    'description': 'Сообщество разработчиков и IT-специалистов',
                    'city': 'Москва',
                    'tags': ['IT', 'программирование', 'технологии']
                },
                {
                    'title': 'Art Space Gallery',
                    'description': 'Пространство для творчества и искусства',
                    'city': 'Москва',
                    'tags': ['искусства', 'творчество', 'дизайн']
                },
                {
                    'title': 'Science Research Hub',
                    'description': 'Научное сообщество и исследовательский центр',
                    'city': 'Москва',
                    'tags': ['наука', 'исследования', 'образование']
                },
                {
                    'title': 'Business Leaders Club',
                    'description': 'Клуб предпринимателей и бизнес-лидеров',
                    'city': 'Москва',
                    'tags': ['бизнес', 'менеджмент', 'карьера']
                },
                {
                    'title': 'Sports & Health Community',
                    'description': 'Сообщество любителей спорта и здорового образа жизни',
                    'city': 'Москва',
                    'tags': ['спорт', 'здоровье', 'фитнес']
                }
            ]

            organizations = []
            for org_data in orgs_data:
                org = Organisation(
                    title=org_data['title'],
                    description=org_data['description'],
                    owner_id=user_id,
                    status='approved',
                    city=org_data['city'],
                    tags=json.dumps(org_data['tags'])
                )
                db.session.add(org)
                organizations.append(org)
                print(f"✅ СОЗДАНА ОРГАНИЗАЦИЯ: {org_data['title']}")

            db.session.commit()
            print("✅ БАЗОВЫЕ ДАННЫЕ СОХРАНЕНЫ!")

            # СОЗДАЕМ 50 ТЕСТОВЫХ МЕРОПРИЯТИЙ
            from datetime import datetime, timedelta

            test_events = [
                # ТЕХНОЛОГИИ И IT (15 мероприятий)
                {
                    'title': 'Хакатон по мобильной разработке',
                    'description': '48-часовой марафон по созданию мобильных приложений',
                    'date_time': datetime.now() + timedelta(days=7),
                    'location': 'Москва, Коворкинг "Точка кипения"',
                    'event_type': 'хакатон',
                    'interest_tags': ['IT', 'программирование', 'мобильная разработка'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'AI Conference 2024',
                    'description': 'Конференция о последних достижениях в области ИИ',
                    'date_time': datetime.now() + timedelta(days=15),
                    'location': 'Москва, Digital October',
                    'event_type': 'конференция',
                    'interest_tags': ['IT', 'искусственный интеллект', 'технологии'],
                    'format_tags': ['офлайн', 'гибрид'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Blockchain Workshop',
                    'description': 'Воркшоп по разработке смарт-контрактов',
                    'date_time': datetime.now() + timedelta(days=8),
                    'location': None,
                    'event_type': 'воркшоп',
                    'interest_tags': ['IT', 'блокчейн', 'программирование'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'IoT Hackathon',
                    'description': 'Создание IoT-решений для умных городов',
                    'date_time': datetime.now() + timedelta(days=20),
                    'location': 'Москва, Технопарк Сколково',
                    'event_type': 'хакатон',
                    'interest_tags': ['IT', 'IoT', 'инжинерия'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'DevOps Meetup',
                    'description': 'Встреча разработчиков для обсуждения DevOps',
                    'date_time': datetime.now() + timedelta(days=6),
                    'location': 'Москва, Офис Yandex Cloud',
                    'event_type': 'встреча',
                    'interest_tags': ['IT', 'DevOps', 'программирование'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Data Science Bootcamp',
                    'description': 'Интенсивный курс по анализу данных',
                    'date_time': datetime.now() + timedelta(days=12),
                    'location': None,
                    'event_type': 'мастер-класс',
                    'interest_tags': ['IT', 'анализ данных', 'наука'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'VR/AR Exhibition',
                    'description': 'Выставка технологий виртуальной реальности',
                    'date_time': datetime.now() + timedelta(days=18),
                    'location': 'Москва, ЦВЗ Манеж',
                    'event_type': 'выставка',
                    'interest_tags': ['IT', 'VR/AR', 'технологии'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Cybersecurity Seminar',
                    'description': 'Семинар по методам кибербезопасности',
                    'date_time': datetime.now() + timedelta(days=9),
                    'location': None,
                    'event_type': 'семинар',
                    'interest_tags': ['IT', 'кибербезопасность', 'бизнес'],
                    'format_tags': ['гибрид'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Startup Pitch Night',
                    'description': 'Презентация IT-стартапов перед инвесторами',
                    'date_time': datetime.now() + timedelta(days=14),
                    'location': 'Москва, Коворкинг "Старт"',
                    'event_type': 'встреча',
                    'interest_tags': ['IT', 'стартапы', 'бизнес'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Web Development Marathon',
                    'description': 'Марафон по веб-разработке',
                    'date_time': datetime.now() + timedelta(days=25),
                    'location': 'Москва, Офис VK',
                    'event_type': 'хакатон',
                    'interest_tags': ['IT', 'веб-разработка', 'программирование'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Cloud Technologies Summit',
                    'description': 'Саммит по облачным технологиям',
                    'date_time': datetime.now() + timedelta(days=30),
                    'location': 'Москва, Крокус Сити Холл',
                    'event_type': 'конференция',
                    'interest_tags': ['IT', 'облачные технологии', 'инфраструктура'],
                    'format_tags': ['офлайн', 'гибрид'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Mobile Apps Design Competition',
                    'description': 'Конкурс дизайна мобильных приложений',
                    'date_time': datetime.now() + timedelta(days=22),
                    'location': None,
                    'event_type': 'конкурс',
                    'interest_tags': ['IT', 'дизайн', 'мобильная разработка'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Python Developers Meeting',
                    'description': 'Встреча Python-разработчиков',
                    'date_time': datetime.now() + timedelta(days=4),
                    'location': 'Москва, Бар "Код"',
                    'event_type': 'встреча',
                    'interest_tags': ['IT', 'Python', 'программирование'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Game Development Workshop',
                    'description': 'Воркшоп по разработке игр на Unity',
                    'date_time': datetime.now() + timedelta(days=11),
                    'location': None,
                    'event_type': 'воркшоп',
                    'interest_tags': ['IT', 'геймдев', 'программирование'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'IT Career Fair 2024',
                    'description': 'Ярмарка вакансий от IT компаний',
                    'date_time': datetime.now() + timedelta(days=28),
                    'location': 'Москва, ЦВК Экспоцентр',
                    'event_type': 'выставка',
                    'interest_tags': ['IT', 'карьера', 'бизнес'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[0].id
                },

                # ИСКУССТВО И КУЛЬТУРА (10 мероприятий)
                {
                    'title': 'Contemporary Art Festival',
                    'description': 'Фестиваль современного искусства',
                    'date_time': datetime.now() + timedelta(days=16),
                    'location': 'Москва, ММОМА',
                    'event_type': 'фестиваль',
                    'interest_tags': ['искусства', 'творчество', 'культура'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Digital Painting Masterclass',
                    'description': 'Мастер-класс по цифровой живописи',
                    'date_time': datetime.now() + timedelta(days=7),
                    'location': None,
                    'event_type': 'мастер-класс',
                    'interest_tags': ['искусства', 'дизайн', 'творчество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Jazz Concert',
                    'description': 'Концерт классического джаза',
                    'date_time': datetime.now() + timedelta(days=11),
                    'location': 'Москва, Джаз-клуб',
                    'event_type': 'концерт',
                    'interest_tags': ['музыка', 'культура', 'искусства'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Photography Exhibition',
                    'description': 'Выставка уличной фотографии',
                    'date_time': datetime.now() + timedelta(days=13),
                    'location': 'Москва, Галерея "Фотолофт"',
                    'event_type': 'выставка',
                    'interest_tags': ['искусства', 'фотография', 'творчество'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Creative Writing Workshop',
                    'description': 'Воркшоп по креативному письму',
                    'date_time': datetime.now() + timedelta(days=10),
                    'location': None,
                    'event_type': 'воркшоп',
                    'interest_tags': ['творчество', 'письмо', 'искусства'],
                    'format_tags': ['гибрид'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Theatre Performance',
                    'description': 'Современная интерпретация классики',
                    'date_time': datetime.now() + timedelta(days=19),
                    'location': 'Москва, Театр.doc',
                    'event_type': 'концерт',
                    'interest_tags': ['культура', 'искусства', 'театр'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Street Art Festival',
                    'description': 'Фестиваль уличного искусства',
                    'date_time': datetime.now() + timedelta(days=24),
                    'location': 'Москва, Арт-квартал',
                    'event_type': 'фестиваль',
                    'interest_tags': ['искусства', 'граффити', 'культура'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Pottery Workshop',
                    'description': 'Мастер-класс по гончарному искусству',
                    'date_time': datetime.now() + timedelta(days=8),
                    'location': 'Москва, Студия "Глина"',
                    'event_type': 'мастер-класс',
                    'interest_tags': ['искусства', 'ремесло', 'творчество'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Digital Art Competition',
                    'description': 'Конкурс цифрового искусства',
                    'date_time': datetime.now() + timedelta(days=26),
                    'location': None,
                    'event_type': 'конкурс',
                    'interest_tags': ['искусства', 'NFT', 'технологии'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Classical Music Evening',
                    'description': 'Вечер классической музыки',
                    'date_time': datetime.now() + timedelta(days=17),
                    'location': 'Москва, Консерватория',
                    'event_type': 'концерт',
                    'interest_tags': ['музыка', 'культура', 'искусства'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[1].id
                },

                # НАУКА И ОБРАЗОВАНИЕ (10 мероприятий)
                {
                    'title': 'Science Slam',
                    'description': 'Битва ученых в формате стендапа',
                    'date_time': datetime.now() + timedelta(days=8),
                    'location': 'Москва, Бар "Научка"',
                    'event_type': 'фестиваль',
                    'interest_tags': ['наука', 'образование', 'исследования'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Astronomy Lecture',
                    'description': 'Лекция о темной материи',
                    'date_time': datetime.now() + timedelta(days=5),
                    'location': None,
                    'event_type': 'лекция',
                    'interest_tags': ['наука', 'астрономия', 'исследования'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Biology Workshop',
                    'description': 'Семинар по генной инженерии',
                    'date_time': datetime.now() + timedelta(days=15),
                    'location': 'Москва, Лаборатория "БиоТех"',
                    'event_type': 'воркшоп',
                    'interest_tags': ['наука', 'биология', 'исследования'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Physics Competition',
                    'description': 'Олимпиада по квантовой физике',
                    'date_time': datetime.now() + timedelta(days=22),
                    'location': 'Москва, МФТИ',
                    'event_type': 'конкурс',
                    'interest_tags': ['наука', 'физика', 'образование'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Science Communication Seminar',
                    'description': 'Семинар по научной коммуникации',
                    'date_time': datetime.now() + timedelta(days=12),
                    'location': None,
                    'event_type': 'семинар',
                    'interest_tags': ['наука', 'коммуникация', 'образование'],
                    'format_tags': ['гибрид'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Mathematics Olympiad',
                    'description': 'Региональный тур олимпиады',
                    'date_time': datetime.now() + timedelta(days=19),
                    'location': 'Москва, МГУ',
                    'event_type': 'конкурс',
                    'interest_tags': ['наука', 'математика', 'образование'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Chemistry Show',
                    'description': 'Шоу химических экспериментов',
                    'date_time': datetime.now() + timedelta(days=14),
                    'location': 'Москва, Парк "Зарядье"',
                    'event_type': 'фестиваль',
                    'interest_tags': ['наука', 'химия', 'образование'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Robotics Workshop',
                    'description': 'Создание роботов своими руками',
                    'date_time': datetime.now() + timedelta(days=9),
                    'location': 'Москва, Технопарк',
                    'event_type': 'воркшоп',
                    'interest_tags': ['наука', 'робототехника', 'инжинерия'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Environmental Science Conference',
                    'description': 'Конференция по проблемам экологии',
                    'date_time': datetime.now() + timedelta(days=27),
                    'location': 'Москва, РАН',
                    'event_type': 'конференция',
                    'interest_tags': ['наука', 'экология', 'исследования'],
                    'format_tags': ['офлайн', 'гибрид'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Psychology Lecture',
                    'description': 'Лекция о когнитивных искажениях',
                    'date_time': datetime.now() + timedelta(days=6),
                    'location': None,
                    'event_type': 'лекция',
                    'interest_tags': ['наука', 'психология', 'образование'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },

                # КАРЬЕРА И БИЗНЕС (8 мероприятий)
                {
                    'title': 'Career Fair: IT карьера 2024',
                    'description': 'Ярмарка вакансий от IT компаний',
                    'date_time': datetime.now() + timedelta(days=17),
                    'location': 'Москва, ЦВК Экспоцентр',
                    'event_type': 'выставка',
                    'interest_tags': ['карьера', 'IT', 'бизнес'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Business Networking',
                    'description': 'Утренний кофе с предпринимателями',
                    'date_time': datetime.now() + timedelta(days=4),
                    'location': 'Москва, Кофейня "Бизнес завтрак"',
                    'event_type': 'встреча',
                    'interest_tags': ['бизнес', 'нетворкинг', 'менеджмент'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'MBA Info Session',
                    'description': 'Информационная сессия о программах MBA',
                    'date_time': datetime.now() + timedelta(days=9),
                    'location': None,
                    'event_type': 'семинар',
                    'interest_tags': ['образование', 'бизнес', 'карьера'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Leadership Workshop',
                    'description': 'Воркшоп по развитию лидерских качеств',
                    'date_time': datetime.now() + timedelta(days=14),
                    'location': 'Москва, Бизнес-центр "Сити"',
                    'event_type': 'воркшоп',
                    'interest_tags': ['менеджмент', 'лидерство', 'бизнес'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Startup Investment Pitch',
                    'description': 'Презентация стартапов перед инвесторами',
                    'date_time': datetime.now() + timedelta(days=21),
                    'location': 'Москва, Коворкинг "Сколково"',
                    'event_type': 'встреча',
                    'interest_tags': ['бизнес', 'стартапы', 'инвестиции'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Digital Marketing Conference',
                    'description': 'Конференция по цифровому маркетингу',
                    'date_time': datetime.now() + timedelta(days=29),
                    'location': 'Москва, World Trade Center',
                    'event_type': 'конференция',
                    'interest_tags': ['бизнес', 'маркетинг', 'технологии'],
                    'format_tags': ['офлайн', 'гибрид'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Financial Planning Seminar',
                    'description': 'Семинар по финансовому планированию',
                    'date_time': datetime.now() + timedelta(days=11),
                    'location': None,
                    'event_type': 'семинар',
                    'interest_tags': ['бизнес', 'финансы', 'образование'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Business English Workshop',
                    'description': 'Воркшоп по деловому английскому',
                    'date_time': datetime.now() + timedelta(days=13),
                    'location': 'Москва, Языковой центр',
                    'event_type': 'воркшоп',
                    'interest_tags': ['бизнес', 'языки', 'образование'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[3].id
                },

                # ЗДОРОВЬЕ И СПОРТ (7 мероприятий)
                {
                    'title': 'Yoga Marathon',
                    'description': '24 часа йоги в городе',
                    'date_time': datetime.now() + timedelta(days=21),
                    'location': 'Москва, Парк Горького',
                    'event_type': 'фестиваль',
                    'interest_tags': ['спорт', 'здоровье', 'йога'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[4].id
                },
                {
                    'title': 'Nutrition Seminar',
                    'description': 'Семинар о правильном питании',
                    'date_time': datetime.now() + timedelta(days=6),
                    'location': None,
                    'event_type': 'семинар',
                    'interest_tags': ['здоровье', 'наука', 'питание'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[4].id
                },
                {
                    'title': 'Urban Sports Festival',
                    'description': 'Фестиваль уличных видов спорта',
                    'date_time': datetime.now() + timedelta(days=25),
                    'location': 'Москва, ВДНХ',
                    'event_type': 'фестиваль',
                    'interest_tags': ['спорт', 'культура', 'сообщество'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[4].id
                },
                {
                    'title': 'Mental Health Workshop',
                    'description': 'Воркшоп по управлению стрессом',
                    'date_time': datetime.now() + timedelta(days=10),
                    'location': 'Москва, Центр психологического здоровья',
                    'event_type': 'воркшоп',
                    'interest_tags': ['здоровье', 'психология', 'саморазвитие'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[4].id
                },
                {
                    'title': 'Running Championship',
                    'description': 'Московский марафон',
                    'date_time': datetime.now() + timedelta(days=32),
                    'location': 'Москва, Лужники',
                    'event_type': 'чемпионат',
                    'interest_tags': ['спорт', 'здоровье', 'сообщество'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[4].id
                },
                {
                    'title': 'Meditation Retreat',
                    'description': 'Выездной ретрит с практиками медитации',
                    'date_time': datetime.now() + timedelta(days=35),
                    'location': 'Московская область, Эко-отель',
                    'event_type': 'фестиваль',
                    'interest_tags': ['здоровье', 'психология', 'отдых'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[4].id
                },
                {
                    'title': 'Fitness Technology Expo',
                    'description': 'Выставка технологий в фитнесе',
                    'date_time': datetime.now() + timedelta(days=23),
                    'location': 'Москва, Сокольники',
                    'event_type': 'выставка',
                    'interest_tags': ['спорт', 'технологии', 'здоровье'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[4].id
                }
            ]

            # Создаем мероприятия
            for event_data in test_events:
                event = PostEvent(
                    title=event_data['title'],
                    description=event_data['description'],
                    date_time=event_data['date_time'],
                    location=event_data['location'],
                    event_type=event_data['event_type'],
                    organization_id=event_data['org_id']
                )
                event.set_interest_tags(event_data['interest_tags'])
                event.set_format_tags(event_data['format_tags'])
                db.session.add(event)

            db.session.commit()
            print(f"✅ СОЗДАНО {len(test_events)} ТЕСТОВЫХ МЕРОПРИЯТИЙ!")

            # СОЗДАЕМ 50 ПРОСТЫХ ПОСТОВ
            simple_posts = [
                # ТЕХНОЛОГИИ И IT (15 постов)
                {
                    'title': 'Новости IT сообщества',
                    'description': 'В этом месяце мы запускаем несколько новых проектов в области веб-разработки и мобильных приложений...',
                    'interest_tags': ['IT', 'технологии', 'программирование'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Итоги года в IT индустрии',
                    'description': 'Обзор ключевых событий и трендов в мире информационных технологий за прошедший год...',
                    'interest_tags': ['IT', 'технологии', 'бизнес'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Python для начинающих: с чего начать',
                    'description': 'Подробное руководство по изучению Python для тех, кто только начинает свой путь в программировании...',
                    'interest_tags': ['IT', 'программирование', 'образование'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Будущее облачных технологий',
                    'description': 'Как облачные вычисления изменят подход к разработке программного обеспечения в ближайшие годы...',
                    'interest_tags': ['IT', 'технологии', 'инновации'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Карьера в Data Science: советы экспертов',
                    'description': 'Интервью с ведущими специалистами в области анализа данных о том, как построить успешную карьеру...',
                    'interest_tags': ['IT', 'наука', 'карьера'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Кибербезопасность в современном мире',
                    'description': 'Важность защиты данных и практические советы по обеспечению кибербезопасности для бизнеса...',
                    'interest_tags': ['IT', 'безопасность', 'бизнес'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Мобильная разработка: тренды 2024',
                    'description': 'Обзор новых технологий и подходов в создании мобильных приложений для iOS и Android...',
                    'interest_tags': ['IT', 'мобильная разработка', 'технологии'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Open Source проекты для начинающих',
                    'description': 'Список открытых проектов, куда можно внести свой вклад и получить ценный опыт...',
                    'interest_tags': ['IT', 'программирование', 'сообщество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'DevOps практики для небольших команд',
                    'description': 'Как внедрить DevOps методологии в работу маленьких и средних разработческих команд...',
                    'interest_tags': ['IT', 'DevOps', 'менеджмент'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Искусственный интеллект в медицине',
                    'description': 'Как AI помогает в диагностике заболеваний и разработке новых лекарственных препаратов...',
                    'interest_tags': ['IT', 'искусственный интеллект', 'здоровье'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Веб-доступность: почему это важно',
                    'description': 'Принципы создания доступных веб-сайтов для людей с ограниченными возможностями...',
                    'interest_tags': ['IT', 'веб-разработка', 'дизайн'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Blockchain за пределами криптовалют',
                    'description': 'Практические применения блокчейн-технологий в различных отраслях экономики...',
                    'interest_tags': ['IT', 'блокчейн', 'инновации'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Тестирование программного обеспечения',
                    'description': 'Лучшие практики и инструменты для обеспечения качества программных продуктов...',
                    'interest_tags': ['IT', 'тестирование', 'качество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'UI/UX дизайн для разработчиков',
                    'description': 'Основы пользовательского интерфейса и опыта, которые должен знать каждый программист...',
                    'interest_tags': ['IT', 'дизайн', 'пользовательский опыт'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },
                {
                    'title': 'Микросервисная архитектура: плюсы и минусы',
                    'description': 'Когда стоит использовать микросервисы и какие подводные камни могут ожидать...',
                    'interest_tags': ['IT', 'архитектура', 'разработка'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[0].id
                },

                # ИСКУССТВО И КУЛЬТУРА (15 постов)
                {
                    'title': 'Арт-выставка "Будущее сейчас"',
                    'description': 'Приглашаем на открытие новой выставки современного искусства с участием молодых художников...',
                    'interest_tags': ['искусства', 'творчество', 'культура'],
                    'format_tags': ['офлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'История импрессионизма',
                    'description': 'Как направление импрессионизма изменило представление об искусстве и повлияло на современную живопись...',
                    'interest_tags': ['искусства', 'история', 'культура'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Цифровое искусство: новые горизонты',
                    'description': 'Обзор современных технологий в создании цифрового искусства и NFT...',
                    'interest_tags': ['искусства', 'технологии', 'творчество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Фотография как искусство',
                    'description': 'Как обычные фотографии превращаются в художественные произведения...',
                    'interest_tags': ['искусства', 'фотография', 'творчество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Современный театр: вызовы и возможности',
                    'description': 'Как цифровые технологии меняют театральное искусство...',
                    'interest_tags': ['искусства', 'театр', 'культура'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Уличное искусство: вандализм или искусство?',
                    'description': 'Дискуссия о месте уличного искусства в современной культуре...',
                    'interest_tags': ['искусства', 'граффити', 'культура'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Искусство Востока: традиции и современность',
                    'description': 'Влияние восточных художественных традиций на современное искусство...',
                    'interest_tags': ['искусства', 'культура', 'традиции'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Керамика: от ремесла к искусству',
                    'description': 'История развития керамического искусства от древности до наших дней...',
                    'interest_tags': ['искусства', 'ремесло', 'творчество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Музыкальные инновации в классической музыке',
                    'description': 'Как современные композиторы экспериментируют с классическими формами...',
                    'interest_tags': ['музыка', 'искусства', 'инновации'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Дизайн интерьера: искусство и функциональность',
                    'description': 'Как создать гармоничное пространство, сочетающее красоту и практичность...',
                    'interest_tags': ['дизайн', 'искусства', 'творчество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Комиксы как форма современного искусства',
                    'description': 'Эволюция комиксов от развлекательного жанра к серьезному искусству...',
                    'interest_tags': ['искусства', 'комиксы', 'культура'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Искусство перформанса: границы возможного',
                    'description': 'Как перформанс стал одной из самых провокационных форм современного искусства...',
                    'interest_tags': ['искусства', 'перформанс', 'творчество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Кинематограф: от немого кино до VR',
                    'description': 'Эволюция кинематографа и влияние технологий на киноискусство...',
                    'interest_tags': ['кино', 'искусства', 'технологии'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Мода как искусство',
                    'description': 'Как мода превратилась из утилитарной необходимости в форму художественного выражения...',
                    'interest_tags': ['мода', 'искусства', 'дизайн'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Искусство реставрации: сохраняя наследие',
                    'description': 'Современные методы реставрации произведений искусства и исторических памятников...',
                    'interest_tags': ['искусства', 'история', 'культура'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },

                # НАУКА И ОБРАЗОВАНИЕ (10 постов)
                {
                    'title': 'Научные открытия 2024',
                    'description': 'Обзор самых значимых научных достижений этого года в различных областях знаний...',
                    'interest_tags': ['наука', 'исследования', 'инновации'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Квантовые вычисления: прорыв в технологиях',
                    'description': 'Как квантовые компьютеры могут изменить наше представление о вычислениях...',
                    'interest_tags': ['наука', 'технологии', 'физика'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Исследования космоса: новые горизонты',
                    'description': 'Современные космические миссии и их значение для человечества...',
                    'interest_tags': ['наука', 'космос', 'исследования'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Биотехнологии в сельском хозяйстве',
                    'description': 'Как генная инженерия помогает решать проблемы продовольственной безопасности...',
                    'interest_tags': ['наука', 'биология', 'инновации'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Психология обучения: как мы учимся',
                    'description': 'Современные исследования в области когнитивной психологии и их применение в образовании...',
                    'interest_tags': ['наука', 'психология', 'образование'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Изменение климата: факты и решения',
                    'description': 'Научный взгляд на проблему изменения климата и возможные пути ее решения...',
                    'interest_tags': ['наука', 'экология', 'общество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Нейробиология сознания',
                    'description': 'Что современная наука знает о природе сознания и работе мозга...',
                    'interest_tags': ['наука', 'нейробиология', 'психология'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Археологические открытия года',
                    'description': 'Самые значимые археологические находки, изменившие наши представления о истории...',
                    'interest_tags': ['наука', 'археология', 'история'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Нанотехнологии в медицине',
                    'description': 'Как наночастицы используются для диагностики и лечения заболеваний...',
                    'interest_tags': ['наука', 'медицина', 'технологии'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },
                {
                    'title': 'Математика в современном мире',
                    'description': 'Применение математических методов в различных сферах жизни и науки...',
                    'interest_tags': ['наука', 'математика', 'образование'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[2].id
                },

                # КАРЬЕРА И БИЗНЕС (5 постов)
                {
                    'title': 'Удаленная работа: новые вызовы',
                    'description': 'Как эффективно организовать удаленную работу и сохранить продуктивность команды...',
                    'interest_tags': ['карьера', 'бизнес', 'менеджмент'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Стартапы: от идеи к успеху',
                    'description': 'Истории успешных стартапов и советы для начинающих предпринимателей...',
                    'interest_tags': ['бизнес', 'стартапы', 'инновации'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Личный бренд в цифровую эпоху',
                    'description': 'Как построить сильный личный бренд и использовать его для карьерного роста...',
                    'interest_tags': ['карьера', 'бизнес', 'маркетинг'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Эмоциональный интеллект в бизнесе',
                    'description': 'Как развитие эмоционального интеллекта помогает в управлении и карьере...',
                    'interest_tags': ['бизнес', 'психология', 'менеджмент'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[3].id
                },
                {
                    'title': 'Устойчивое развитие бизнеса',
                    'description': 'Как компании могут сочетать прибыльность с социальной и экологической ответственностью...',
                    'interest_tags': ['бизнес', 'экология', 'общество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[3].id
                },

                # ЗДОРОВЬЕ И СПОРТ (3 поста)
                {
                    'title': 'Ментальное здоровье в современном мире',
                    'description': 'Важность заботы о ментальном здоровье и практические техники для его поддержания...',
                    'interest_tags': ['здоровье', 'психология', 'общество'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[4].id
                },
                {
                    'title': 'Спорт и технологии: новые возможности',
                    'description': 'Как современные технологии помогают спортсменам достигать лучших результатов...',
                    'interest_tags': ['спорт', 'технологии', 'здоровье'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[4].id
                },
                {
                    'title': 'Питание для мозга',
                    'description': 'Какие продукты помогают улучшить когнитивные функции и поддерживать мозг здоровым...',
                    'interest_tags': ['здоровье', 'питание', 'наука'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[4].id
                },

                # ПУТЕШЕСТВИЯ И ЯЗЫКИ (2 поста)
                {
                    'title': 'Изучение языков: эффективные методы',
                    'description': 'Современные подходы к изучению иностранных языков и преодолению языкового барьера...',
                    'interest_tags': ['языки', 'образование', 'культура'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                },
                {
                    'title': 'Экотуризм: путешествия с заботой о природе',
                    'description': 'Как путешествовать, минимизируя негативное воздействие на окружающую среду...',
                    'interest_tags': ['путешествия', 'экология', 'культура'],
                    'format_tags': ['онлайн'],
                    'org_id': organizations[1].id
                }
            ]

            # Создаем посты
            for post_data in simple_posts:
                post = PostSimple(
                    title=post_data['title'],
                    description=post_data['description'],
                    organization_id=post_data['org_id']
                )
                post.set_interest_tags(post_data['interest_tags'])
                post.set_format_tags(post_data['format_tags'])
                db.session.add(post)

            db.session.commit()
            print(f"✅ СОЗДАНО {len(simple_posts)} ПРОСТЫХ ПОСТОВ!")

            print("🎉 ИНИЦИАЛИЗАЦИЯ БАЗЫ ДАННЫХ УСПЕШНО ЗАВЕРШЕНА!")
        else:
            print("📊 БАЗА ДАННЫХ УЖЕ СОДЕРЖИТ ДАННЫЕ - пропускаем инициализацию")

        # Всегда показываем статистику
        print(f"\n📊 ТЕКУЩАЯ СТАТИСТИКА БАЗЫ ДАННЫХ:")
        print(f"   👤 Пользователей: {User.query.count()}")
        print(f"   🏢 Организаций: {Organisation.query.count()}")
        print(f"   📅 Мероприятий: {PostEvent.query.count()}")
        print(f"   📝 Постов: {PostSimple.query.count()}")
        print(f"   🏆 Достижений: {Achievement.query.count()}")

    except Exception as e:
        print(f"❌ КРИТИЧЕСКАЯ ОШИБКА ПРИ ИНИЦИАЛИЗАЦИИ БД: {e}")
        import traceback

        traceback.print_exc()
        db.session.rollback()




# Маршруты
@app.route('/')
def home():
    return jsonify({"message": "Flask Auth API работает! 🚀", "status": "running"})


@app.route('/api/register', methods=['POST'])
def register():
    try:
        if not request.is_json:
            return jsonify({"error": "Missing JSON in request"}), 400

        data = request.get_json()

        required_fields = ['email', 'password', 'first_name', 'last_name']
        missing_fields = [field for field in required_fields if not data.get(field)]

        if missing_fields:
            return jsonify({"error": f"Обязательные поля отсутствуют: {', '.join(missing_fields)}"}), 400

        # Валидация email
        is_valid_email, email_error = validate_email(data['email'])
        if not is_valid_email:
            return jsonify({"error": email_error}), 400

        # Валидация пароля
        is_valid_password, password_error = validate_password(data['password'])
        if not is_valid_password:
            return jsonify({"error": password_error}), 400

        existing_user = User.query.filter_by(email=data['email']).first()
        if existing_user:
            return jsonify({"error": "Пользователь с таким email уже существует"}), 400

        user = User(
            email=data['email'],
            first_name=data['first_name'],
            last_name=data['last_name']
        )
        user.set_password(data['password'])

        # Устанавливаем начальные метрики
        initial_interests = {
            'IT': 0.1, 'искусства': 0.1, 'музыка': 0.1, 'языки': 0.1,
            'экономика': 0.1, 'менеджмент': 0.1, 'творчество': 0.1,
            'спорт': 0.1, 'инжинерия': 0.1, 'культура': 0.1
        }
        user.set_interests_metrics(initial_interests)

        initial_formats = {'онлайн': 0.33, 'офлайн': 0.33, 'гибрид': 0.34}
        user.set_format_metrics(initial_formats)

        initial_event_types = {}
        user.set_event_type_metrics(initial_event_types)

        db.session.add(user)
        db.session.commit()

        access_token = create_access_token(identity=user.id)

        return jsonify({
            "message": "Пользователь создан успешно!",
            "user": user.to_dict(),
            "access_token": access_token,
            "next_step": "complete_profile"  # Указываем следующий шаг
        }), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": f"Внутренняя ошибка сервера: {str(e)}"}), 500


@app.route('/api/users/<int:user_id>/complete-preferences', methods=['POST'])
@jwt_required()
def complete_preferences(user_id):
    try:
        print(f"DEBUG: Complete preferences request for user {user_id}")

        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        data = request.get_json()
        print(f"DEBUG: Received preferences data: {data}")

        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        # Получаем выбранные предпочтения
        interests = data.get('interests', [])
        formats = data.get('formats', [])
        event_types = data.get('event_types', [])

        print(f"DEBUG: Selected - Interests: {interests}, Formats: {formats}, Event Types: {event_types}")

        # ВАЖНО: Обновляем метрики интересов на основе выбора пользователя
        # Создаем новые метрики с высокими весами для выбранных интересов
        interests_metrics = {}
        for interest in interests:
            interests_metrics[interest] = 0.8  # Высокий вес для выбранных интересов

        # Добавляем базовые веса для остальных категорий
        all_categories = INTEREST_CATEGORIES
        for category in all_categories:
            if category not in interests_metrics:
                interests_metrics[category] = 0.1  # Низкий вес для невыбранных

        formats_metrics = {}
        for format_type in formats:
            formats_metrics[format_type] = 0.8

        # Добавляем базовые веса для форматов
        all_formats = FORMAT_TYPES
        for format_type in all_formats:
            if format_type not in formats_metrics:
                formats_metrics[format_type] = 0.1

        event_type_metrics = {}
        for event_type in event_types:
            event_type_metrics[event_type] = 0.8

        # Добавляем базовые веса для типов событий
        all_event_types = EVENT_TYPES
        for event_type in all_event_types:
            if event_type not in event_type_metrics:
                event_type_metrics[event_type] = 0.1

        # Нормализуем метрики
        interests_metrics = normalize_metrics(interests_metrics)
        formats_metrics = normalize_metrics(formats_metrics)
        event_type_metrics = normalize_metrics(event_type_metrics)

        print(f"DEBUG: Updated interests_metrics: {interests_metrics}")
        print(f"DEBUG: Updated formats_metrics: {formats_metrics}")
        print(f"DEBUG: Updated event_type_metrics: {event_type_metrics}")

        # Сохраняем обновленные метрики
        user.set_interests_metrics(interests_metrics)
        user.set_format_metrics(formats_metrics)
        user.set_event_type_metrics(event_type_metrics)
        user.preferences_completed = True

        # Начисляем достижение за завершение опроса
        registration_achievement = Achievement.query.filter_by(name='Регистрация на платформе').first()
        if registration_achievement and registration_achievement not in user.achievements:
            user.achievements.append(registration_achievement)
            user.exp += registration_achievement.points

        db.session.commit()

        # Логируем итоговые метрики для проверки
        print(f"DEBUG: Final user interests: {user.get_interests_metrics()}")
        print(f"DEBUG: Final user formats: {user.get_format_metrics()}")

        response_data = {
            "message": "Предпочтения успешно сохранены",
            "user": user.to_dict(),
            "next_step": "main"
        }
        print(f"DEBUG: Sending response: {response_data}")

        return jsonify(response_data), 200

    except Exception as e:
        print(f"ERROR: Complete preferences error: {e}")
        db.session.rollback()
        return jsonify({"error": str(e)}), 500

@app.route('/api/users/<int:user_id>/complete-profile', methods=['POST'])
@jwt_required()
def complete_profile(user_id):
    try:
        print(f"DEBUG: Complete profile request for user {user_id}")

        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        data = request.get_json()
        print(f"DEBUG: Received data: {data}")

        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        # Обновляем данные профиля
        if 'phone' in data:
            user.phone = data['phone']
        if 'age_user' in data:  # ← принимаем 'age' из фронтенда
            user.age_user = data['age_user']
        if 'placement' in data:
            user.placement = data['placement']
        if 'study_place' in data:
            user.study_place = data['study_place']
        if 'grade_course' in data:
            user.grade_course = data['grade_course']

        user.profile_completed = True  # ← ВАЖНО!

        print(f"DEBUG: User profile_completed set to: {user.profile_completed}")

        db.session.commit()

        response_data = {
            "message": "Профиль успешно обновлен",
            "user": user.to_dict(),
            "next_step": "complete_preferences"
        }
        print(f"DEBUG: Sending response: {response_data}")

        return jsonify(response_data), 200

    except Exception as e:
        print(f"ERROR: Complete profile error: {e}")
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@app.route('/api/users/<int:user_id>/profile', methods=['GET'])
@jwt_required()
def get_user_profile(user_id):
    try:
        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        # Статистика пользователя - ИСПРАВЛЕННАЯ ВЕРСИЯ
        stats = {
            'events_attended': len(user.registered_events),
            'events_created': len([post for post in PostEvent.query.all() if post.organization and post.organization.owner_id == user_id]),  # События организаций пользователя
            'organizations_count': len(user.user_organisations),
            'likes_given': len(user.liked_event_posts),
            'exp': user.exp,
            'level': user.exp // 100
        }

        # Достижения пользователя
        achievements = user.achievements

        return jsonify({
            "user": user.to_dict(),
            "stats": stats,
            "achievements": [{
                'id': ach.id,
                'name': ach.name,
                'description': ach.description,
                'points': ach.points,
                'earned_at': None
            } for ach in achievements]
        }), 200

    except Exception as e:
        print(f"ERROR: Get user profile error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/users/<int:user_id>/profile', methods=['PUT'])
@jwt_required()
def update_user_profile(user_id):
    try:
        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        data = request.get_json()
        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        # Обновляем поля
        if 'phone' in data:
            user.phone = data['phone']
        if 'age_user' in data:
            user.age_user = data['age_user']
        if 'placement' in data:
            user.placement = data['placement']
        if 'study_place' in data:
            user.study_place = data['study_place']
        if 'grade_course' in data:
            user.grade_course = data['grade_course']
        if 'avatar' in data:
            user.avatar = data['avatar']

        db.session.commit()

        # Возвращаем обновленные данные
        stats = {
            'events_attended': len(user.registered_events),
            'events_created': len(PostEvent.query.filter_by(author_id=user_id).all()),
            'organizations_count': len(user.user_organisations),
            'likes_given': len(user.liked_event_posts),
            'exp': user.exp,
            'level': user.exp // 100
        }

        return jsonify({
            "user": user.to_dict(),
            "stats": stats,
            "achievements": [{
                'id': ach.id,
                'name': ach.name,
                'description': ach.description,
                'points': ach.points
            } for ach in user.achievements]
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500

@app.route('/api/users/<int:user_id>/events', methods=['GET'])
@jwt_required()
def get_user_events(user_id):
    try:
        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        print(f"DEBUG: Getting events for user {user_id}")

        # Предстоящие мероприятия (где дата в будущем)
        upcoming_events = []
        for event in user.registered_events:
            if event.date_time and event.date_time > datetime.now():
                upcoming_events.append(event)
                print(f"DEBUG: Upcoming event: {event.title} - {event.date_time}")

        # Прошедшие мероприятия (где дата в прошлом)
        past_events = []
        for event in user.registered_events:
            if event.date_time and event.date_time <= datetime.now():
                past_events.append(event)
                print(f"DEBUG: Past event: {event.title} - {event.date_time}")

        # Созданные мероприятия (мероприятия организаций пользователя)
        created_events = []
        user_organisations = Organisation.query.filter_by(owner_id=user_id).all()
        for org in user_organisations:
            created_events.extend(org.event_posts)
            print(f"DEBUG: Created events from org {org.title}: {len(org.event_posts)}")

        print(f"DEBUG: Events counts - upcoming: {len(upcoming_events)}, past: {len(past_events)}, created: {len(created_events)}")

        return jsonify({
            "upcoming_events": [event.to_dict() for event in upcoming_events],
            "past_events": [event.to_dict() for event in past_events],
            "created_events": [event.to_dict() for event in created_events]
        }), 200

    except Exception as e:
        print(f"ERROR: Get user events error: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": f"Внутренняя ошибка сервера: {str(e)}"}), 500


@app.route('/api/events/<int:event_id>', methods=['GET'])
@jwt_required()
def get_event_details(event_id):
    try:
        event = db.session.get(PostEvent, event_id)
        if not event:
            return jsonify({"error": "Событие не найдено"}), 404

        return jsonify({
            "event": event.to_dict(),
            "success": True
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500





@app.route('/api/search', methods=['POST'])
def search():
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        query = data.get('query')
        filters = data.get('filters', {})
        limit = data.get('limit', 20)
        offset = data.get('offset', 0)

        # Базовый запрос
        events_query = PostEvent.query
        orgs_query = Organisation.query.filter_by(status='approved')

        # Поиск по тексту
        if query:
            search_term = f"%{query}%"
            events_query = events_query.filter(
                db.or_(
                    PostEvent.title.ilike(search_term),
                    PostEvent.description.ilike(search_term)
                )
            )
            orgs_query = orgs_query.filter(
                db.or_(
                    Organisation.title.ilike(search_term),
                    Organisation.description.ilike(search_term)
                )
            )

        # Фильтры по интересам
        if filters.get('interests'):
            interests = filters['interests']
            events_query = events_query.filter(
                PostEvent.interest_tags.contains(json.dumps(interests))
            )

        # Фильтры по форматам
        if filters.get('formats'):
            formats = filters['formats']
            events_query = events_query.filter(
                PostEvent.format_tags.contains(json.dumps(formats))
            )

        # Фильтры по типам событий
        if filters.get('event_types'):
            event_types = filters['event_types']
            events_query = events_query.filter(PostEvent.event_type.in_(event_types))

        # Фильтры по дате
        if filters.get('date_from'):
            try:
                date_from = datetime.fromisoformat(filters['date_from'].replace('Z', '+00:00'))
                events_query = events_query.filter(PostEvent.date_time >= date_from)
            except ValueError:
                pass

        if filters.get('date_to'):
            try:
                date_to = datetime.fromisoformat(filters['date_to'].replace('Z', '+00:00'))
                events_query = events_query.filter(PostEvent.date_time <= date_to)
            except ValueError:
                pass

        # Фильтр по локации
        if filters.get('location'):
            location = f"%{filters['location']}%"
            events_query = events_query.filter(PostEvent.location.ilike(location))

        # Фильтр по организации
        if filters.get('organization_id'):
            events_query = events_query.filter(PostEvent.organization_id == filters['organization_id'])

        # Применяем пагинацию
        events = events_query.offset(offset).limit(limit).all()
        organizations = orgs_query.offset(offset).limit(limit).all()

        return jsonify({
            "events": [event.to_dict() for event in events],
            "organizations": [{
                'id': org.id,
                'title': org.title,
                'description': org.description,
                'avatar': org.avatar,
                'city': org.city,
                'tags': json.loads(org.tags) if org.tags else [],
                'events_count': len(org.event_posts),
                'subscribers_count': len(org.subscribers)
            } for org in organizations],
            "total_events": events_query.count(),
            "total_organizations": orgs_query.count()
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/api/search/suggestions', methods=['GET'])
def get_search_suggestions():
    try:
        # Популярные поисковые запросы (можно брать из статистики)
        popular_searches = [
            "хакатон", "лекция", "мастер-класс", "IT", "программирование",
            "дизайн", "бизнес", "стартапы", "искусство", "наука"
        ]

        # Популярные теги из мероприятий
        popular_tags = []
        events = PostEvent.query.limit(50).all()
        for event in events:
            popular_tags.extend(event.get_interest_tags())

        popular_tags = list(set(popular_tags))[:10]

        return jsonify({
            "popular_searches": popular_searches,
            "recent_searches": [],  # Можно хранить в сессии или БД
            "popular_tags": popular_tags
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500



@app.route('/api/organisations/<int:org_id>/events', methods=['POST'])
@jwt_required()
def create_event(org_id):
    try:
        current_user_id = get_jwt_identity()

        # Проверяем существование организации
        organisation = db.session.get(Organisation, org_id)
        if not organisation:
            return jsonify({"error": "Организация не найдена"}), 404

        # Проверяем права доступа (только владелец может создавать мероприятия)
        if organisation.owner_id != current_user_id:
            return jsonify({"error": "У вас нет прав для создания мероприятий от этой организации"}), 403

        # Проверяем статус организации (только approved организации могут создавать мероприятия)
        if organisation.status != 'approved':
            return jsonify({"error": "Организация не прошла модерацию и не может создавать мероприятия"}), 403

        data = request.get_json()
        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        # Обязательные поля
        required_fields = ['title', 'description', 'date_time']
        missing_fields = [field for field in required_fields if not data.get(field)]

        if missing_fields:
            return jsonify({"error": f"Обязательные поля отсутствуют: {', '.join(missing_fields)}"}), 400

        # Создаем мероприятие
        event = PostEvent(
            title=data['title'],
            description=data['description'],
            date_time=datetime.fromisoformat(data['date_time'].replace('Z', '+00:00')),
            location=data.get('location'),
            event_type=data.get('event_type'),
            organization_id=org_id
        )

        # Устанавливаем теги
        if data.get('interest_tags'):
            event.set_interest_tags(data['interest_tags'])

        if data.get('format_tags'):
            event.set_format_tags(data['format_tags'])

        # Изображение мероприятия
        if data.get('pic'):
            event.pic = data['pic']

        db.session.add(event)
        db.session.commit()

        return jsonify({
            "message": "Мероприятие успешно создано",
            "event": event.to_dict()
        }), 201

    except ValueError as e:
        db.session.rollback()
        return jsonify({"error": f"Неверный формат даты: {str(e)}"}), 400
    except Exception as e:
        db.session.rollback()
        return jsonify({"error": f"Внутренняя ошибка сервера: {str(e)}"}), 500


@app.route('/api/organisations/<int:org_id>/events', methods=['GET'])
def get_organization_events(org_id):
    try:
        organisation = db.session.get(Organisation, org_id)
        if not organisation:
            return jsonify({"error": "Организация не найдена"}), 404

        # Параметры пагинации
        limit = request.args.get('limit', 20, type=int)
        offset = request.args.get('offset', 0, type=int)

        # Получаем мероприятия организации
        events_query = PostEvent.query.filter_by(organization_id=org_id)
        total_events = events_query.count()
        events = events_query.offset(offset).limit(limit).all()

        return jsonify({
            "events": [event.to_dict() for event in events],
            "total": total_events,
            "organization": {
                'id': organisation.id,
                'title': organisation.title,
                'description': organisation.description,
                'avatar': organisation.avatar,
                'city': organisation.city,
                'status': organisation.status
            }
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/preferences/categories', methods=['GET'])
def get_preference_categories():
    \"\"\"Получение категорий для опроса предпочтений\"\"\"
    return jsonify({
        "interest_categories": INTEREST_CATEGORIES,
        "format_types": FORMAT_TYPES,
        "event_types": EVENT_TYPES
    }), 200


@app.route('/api/events/<int:event_id>/register', methods=['POST'])
@jwt_required()
def register_for_event(event_id):
    try:
        user_id = get_jwt_identity()
        user = db.session.get(User, user_id)
        event = db.session.get(PostEvent, event_id)

        if not user or not event:
            return jsonify({"error": "Пользователь или событие не найдены"}), 404

        # Проверяем, не зарегистрирован ли уже пользователь
        if user in event.registered_users:
            return jsonify({"error": "Вы уже зарегистрированы на это событие"}), 400

        # Регистрируем пользователя
        event.registered_users.append(user)

        # Обновляем метрики пользователя
        update_user_interests(user, event)

        # Начисляем достижение за первую регистрацию
        if len(user.registered_events) == 1:  # Первая регистрация
            first_event_achievement = Achievement.query.filter_by(name='Первый ивент').first()
            if first_event_achievement and first_event_achievement not in user.achievements:
                user.achievements.append(first_event_achievement)
                user.exp += first_event_achievement.points

        db.session.commit()

        return jsonify({
            "message": "Вы успешно зарегистрированы на событие",
            "event": event.to_dict()
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@app.route('/api/organisations', methods=['POST'])
@jwt_required()
def create_organisation():
    try:
        user_id = get_jwt_identity()
        data = request.get_json()

        if not data or not data.get('title') or not data.get('description'):
            return jsonify({"error": "Название и описание обязательны"}), 400

        # Проверяем уникальность названия
        existing_org = Organisation.query.filter_by(title=data['title']).first()
        if existing_org:
            return jsonify({"error": "Организация с таким названием уже существует"}), 400

        organisation = Organisation(
            title=data['title'],
            description=data['description'],
            owner_id=user_id,
            city=data.get('city'),
            avatar=data.get('avatar'),
            tags=json.dumps(data.get('tags', [])),
            social_links=json.dumps(data.get('social_links', []))
        )

        db.session.add(organisation)
        db.session.commit()

        return jsonify({
            "message": "Организация создана и отправлена на модерацию",
            "organisation": {
                'id': organisation.id,
                'title': organisation.title,
                'description': organisation.description,
                'status': organisation.status,
                'city': organisation.city
            }
        }), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@app.route('/api/organisations/<int:org_id>', methods=['GET'])
def get_organisation(org_id):
    \"\"\"Получение информации об организации\"\"\"
    organisation = db.session.get(Organisation, org_id)
    if not organisation:
        return jsonify({"error": "Организация не найдена"}), 404

    org_data = {
        'id': organisation.id,
        'title': organisation.title,
        'description': organisation.description,
        'avatar': organisation.avatar,
        'city': organisation.city,
        'status': organisation.status,
        'tags': json.loads(organisation.tags) if organisation.tags else [],
        'social_links': json.loads(organisation.social_links) if organisation.social_links else [],
        'events_count': len(organisation.event_posts),
        'subscribers_count': len(organisation.subscribers),
        'owner_id': organisation.owner_id
    }

    return jsonify({"organisation": org_data}), 200


@app.route('/api/organisations/<int:org_id>/subscribe', methods=['POST'])
@jwt_required()
def subscribe_to_organisation(org_id):
    try:
        user_id = get_jwt_identity()
        user = db.session.get(User, user_id)
        organisation = db.session.get(Organisation, org_id)

        if not user or not organisation:
            return jsonify({"error": "Пользователь или организация не найдены"}), 404

        if organisation in user.subscriptions:
            return jsonify({"error": "Вы уже подписаны на эту организацию"}), 400

        user.subscriptions.append(organisation)

        # Начисляем достижение за первую подписку
        if len(user.subscriptions) == 1:
            first_sub_achievement = Achievement.query.filter_by(name='Первая подписка').first()
            if first_sub_achievement and first_sub_achievement not in user.achievements:
                user.achievements.append(first_sub_achievement)
                user.exp += first_sub_achievement.points

        db.session.commit()

        return jsonify({
            "message": "Вы успешно подписались на организацию",
            "subscribers_count": len(organisation.subscribers)
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@app.route('/api/login', methods=['POST'])
def login():
    try:
        if not request.is_json:
            return jsonify({"error": "Missing JSON in request"}), 400

        data = request.get_json()

        if not data.get('email') or not data.get('password'):
            return jsonify({"error": "Email и пароль обязательны"}), 400

        user = User.query.filter_by(email=data['email']).first()

        if user and user.check_password(data['password']):
            access_token = create_access_token(identity=user.id)

            # Определяем следующий шаг для пользователя - ИСПРАВЛЕННАЯ ВЕРСИЯ
            profile_completed = bool(user.profile_completed)  # Гарантируем boolean
            preferences_completed = bool(user.preferences_completed)  # Гарантируем boolean

            print(
                f"DEBUG: Login - profile_completed: {profile_completed}, preferences_completed: {preferences_completed}")

            next_step = "main"
            if not profile_completed:
                next_step = "complete_profile"
            elif not preferences_completed:
                next_step = "complete_preferences"

            print(f"DEBUG: Next step: {next_step}")

            return jsonify({
                "message": "Вход выполнен успешно!",
                "user": user.to_dict(),
                "access_token": access_token,
                "next_step": next_step
            }), 200
        else:
            return jsonify({"error": "Неверный email или пароль"}), 401

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/api/feed/recommended', methods=['GET'])
@jwt_required()  # РАСКОММЕНТИРУЙТЕ ЭТУ СТРОКУ
def get_recommended_feed():
    try:
        # ИСПОЛЬЗУЕМ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ ИЗ JWT
        user_id = get_jwt_identity()
        user = db.session.get(User, user_id)

        if not user:
            print("DEBUG: Authenticated user not found")
            return jsonify({"error": "Пользователь не найден"}), 404

        print(f"DEBUG: Using authenticated user: {user.email} (ID: {user.id})")
        print(
            f"DEBUG: User preferences - profile_completed: {user.profile_completed}, preferences_completed: {user.preferences_completed}")
        print(f"DEBUG: User interests: {user.get_interests_metrics()}")

        # Обрабатываем параметры
        data = request.args or {}

        try:
            limit = int(data.get('limit', 10))
            offset = int(data.get('offset', 0))
        except (ValueError, TypeError):
            limit = 10
            offset = 0

        print(f"DEBUG: Feed request for user {user.id}")
        print(f"DEBUG: Limit: {limit}, Offset: {offset}")

        # Получаем все посты
        event_posts = PostEvent.query.all()
        simple_posts = PostSimple.query.all()
        all_posts = list(event_posts) + list(simple_posts)

        print(f"DEBUG: Found posts: {len(all_posts)}")

        if not all_posts:
            print("DEBUG: No posts found, returning empty list")
            return jsonify({
                "posts": [],
                "count": 0,
                "total": 0,
                "offset": offset,
                "limit": limit,
                "message": "Нет доступных постов"
            }), 200

        # Сортируем по релевантности ДЛЯ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ
        scored_posts = []
        for post in all_posts:
            try:
                score = post.calculate_relevance_score(user)
                print(f"DEBUG: Post '{post.title}' - relevance: {score}")
                scored_posts.append((post, score))
            except Exception as e:
                print(f"WARNING: Relevance calculation error for post {post.id}: {e}")
                scored_posts.append((post, 0.1))

        # Сортируем по убыванию релевантности
        scored_posts.sort(key=lambda x: x[1], reverse=True)

        print(f"DEBUG: After sorting - top-3:")
        for i, (post, score) in enumerate(scored_posts[:3]):
            print(f"DEBUG:   {i + 1}. '{post.title}' - {score}")

        # Применяем пагинацию
        total_posts = len(scored_posts)
        start_idx = min(offset, total_posts)
        end_idx = min(offset + limit, total_posts)

        paginated_posts = scored_posts[start_idx:end_idx]

        print(f"DEBUG: Pagination: {start_idx}-{end_idx} of {total_posts}")

        # Формируем ответ
        feed_posts = []
        for post, score in paginated_posts:
            post_data = post.to_dict()
            post_data['relevance_score'] = round(score, 3)
            feed_posts.append(post_data)

        print(f"DEBUG: Returning {len(feed_posts)} posts")

        response_data = {
            "posts": feed_posts,
            "count": len(feed_posts),
            "total": total_posts,
            "offset": offset,
            "limit": limit,
            "has_more": end_idx < total_posts
        }

        print(f"DEBUG: Final response: {len(feed_posts)} posts, has_more: {end_idx < total_posts}")
        return jsonify(response_data), 200

    except Exception as e:
        print(f"ERROR: Error in get_recommended_feed: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500

@app.route('/api/debug/user-state/<int:user_id>', methods=['GET'])
@jwt_required()
def debug_user_state(user_id):
    \"\"\"Отладочный эндпоинт для проверки состояния пользователя\"\"\"
    current_user_id = get_jwt_identity()
    if current_user_id != user_id:
        return jsonify({"error": "Доступ запрещен"}), 403

    user = db.session.get(User, user_id)
    if not user:
        return jsonify({"error": "Пользователь не найден"}), 404

    return jsonify({
        "user_id": user.id,
        "email": user.email,
        "profile_completed": user.profile_completed,
        "preferences_completed": user.preferences_completed,
        "interests_metrics": user.get_interests_metrics(),
        "format_metrics": user.get_format_metrics(),
        "login_next_step": "complete_profile" if not user.profile_completed else ("complete_preferences" if not user.preferences_completed else "main")
    }), 200
@app.route('/api/posts/like', methods=['POST'])
@jwt_required()
def like_post_with_interests():
    try:
        user_id = get_jwt_identity()
        user = db.session.get(User, user_id)
        data = request.get_json()

        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        if not data or 'post_id' not in data:
            return jsonify({"error": "Отсутствует post_id"}), 400

        post_id = data['post_id']
        post = PostEvent.query.get(post_id) or PostSimple.query.get(post_id)

        if not post:
            return jsonify({"error": "Пост не найден"}), 404

        # Обновляем метрики интересов на основе тегов поста
        update_user_interests(user, post)

        # Обновляем метрики ленты
        user.update_feed_metrics(post, 'like')

        # Добавляем лайк в базу (если это мероприятие)
        if isinstance(post, PostEvent) and user not in post.liked_by:
            post.liked_by.append(user)

        db.session.commit()

        return jsonify({
            "message": "Пост лайкнут и интересы обновлены",
            "post_id": post_id,
            "user_interests": user.get_interests_metrics()
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@app.route('/api/users/<int:user_id>/interests', methods=['GET'])
@jwt_required()
def get_user_interests(user_id):
    try:
        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        return jsonify({
            "interests_metrics": user.get_interests_metrics(),
            "format_metrics": user.get_format_metrics(),
            "feed_metrics": user.get_feed_metrics()
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/api/debug/feed', methods=['GET'])
@jwt_required()
def debug_feed():
    \"\"\"Временный эндпоинт для отладки ленты\"\"\"
    user_id = get_jwt_identity()
    user = db.session.get(User, user_id)

    if not user:
        return jsonify({"error": "Пользователь не найден"}), 404

    # Просто возвращаем все посты без сортировки
    event_posts = PostEvent.query.all()
    simple_posts = PostSimple.query.all()
    all_posts = list(event_posts) + list(simple_posts)

    feed_posts = []
    for post in all_posts:
        post_data = post.to_dict()
        post_data['relevance_score'] = 0.5  # Фиктивный score
        feed_posts.append(post_data)

    return jsonify({
        "posts": feed_posts,
        "count": len(feed_posts),
        "total": len(feed_posts),
        "message": "DEBUG MODE - все посты"
    }), 200


@app.route('/api/feed', methods=['GET'])
@jwt_required()
def get_feed():
    try:
        user_id = get_jwt_identity()
        user = db.session.get(User, user_id)

        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        # Получаем все посты
        event_posts = PostEvent.query.all()
        simple_posts = PostSimple.query.all()
        all_posts = list(event_posts) + list(simple_posts)

        # Сортируем по релевантности
        scored_posts = []
        for post in all_posts:
            score = post.calculate_relevance_score(user)
            scored_posts.append((post, score))

        # Сортируем по убыванию релевантности
        scored_posts.sort(key=lambda x: x[1], reverse=True)

        # Берем топ-5 постов
        top_posts = scored_posts[:5]

        # Формируем ответ
        feed_posts = []
        for post, score in top_posts:
            post_data = post.to_dict()
            post_data['relevance_score'] = round(score, 3)
            feed_posts.append(post_data)

        return jsonify({
            "posts": feed_posts,
            "count": len(feed_posts)
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/api/posts/like/<int:post_id>', methods=['POST'])
@jwt_required()
def like_post(post_id):
    try:
        user_id = get_jwt_identity()
        user = db.session.get(User, user_id)
        post = PostEvent.query.get(post_id) or PostSimple.query.get(post_id)

        if not user or not post:
            return jsonify({"error": "Пользователь или пост не найден"}), 404

        # Обновляем метрики ленты
        user.update_feed_metrics(post, 'like')
        db.session.commit()

        return jsonify({
            "message": "Пост лайкнут",
            "post_id": post_id
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@app.route('/api/users', methods=['GET'])
def get_users():
    users = User.query.all()
    return jsonify({
        "users": [user.to_dict() for user in users],
        "count": len(users)
    })


@app.route('/api/debug/check', methods=['GET'])
def debug_check():
    \"\"\"Проверка всех данных в базе\"\"\"
    users = User.query.all()
    events = PostEvent.query.all()
    posts = PostSimple.query.all()

    result = {
        "users_count": len(users),
        "events_count": len(events),
        "posts_count": len(posts),
        "users": [{"id": u.id, "email": u.email} for u in users],
        "events": [{"id": e.id, "title": e.title, "tags": e.get_interest_tags()} for e in events],
        "posts": [{"id": p.id, "title": p.title, "tags": p.get_interest_tags()} for p in posts]
    }

    return jsonify(result), 200

@app.route('/api/ping', methods=['GET'])
def ping():
    return jsonify({"message": "pong", "status": "ok"}), 200

\"\"\"

# End of original monolith content"""
