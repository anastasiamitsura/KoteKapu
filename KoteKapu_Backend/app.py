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
app.config['SECRET_KEY'] = 'dev-secret-key'
app.config['JWT_SECRET_KEY'] = 'dev-jwt-key'
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
            'profile_completed': self.profile_completed,
            'preferences_completed': self.preferences_completed,
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
        org = Organisation.query.get(self.organization_id)
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
            'likes': len(self.liked_by) if hasattr(self, 'liked_by') else 0,
            'registered_count': len(self.registered_users) if hasattr(self, 'registered_users') else 0
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
            user_interests = user.get_interests_metrics()
            user_formats = user.get_format_metrics()
            user_feed_metrics = user.get_feed_metrics()

            post_interests = self.get_interest_tags()
            post_formats = self.get_format_tags()

            interest_score = sum(user_interests.get(tag, 0.0) for tag in post_interests)
            format_score = sum(user_formats.get(tag, 0.0) for tag in post_formats)
            feed_interest_score = sum(user_feed_metrics['preferred_categories'].get(tag, 0) for tag in post_interests)
            feed_format_score = sum(user_feed_metrics['preferred_formats'].get(tag, 0) for tag in post_formats)

            total_score = (
                    interest_score * 0.4 +
                    format_score * 0.3 +
                    feed_interest_score * 0.2 +
                    feed_format_score * 0.1
            )

            return total_score

        except Exception as e:
            print(f"ERROR: Ошибка в calculate_relevance_score: {e}")
            return 0.1

    def to_dict(self):
        org = Organisation.query.get(self.organization_id) if self.organization_id else None
        author = User.query.get(self.author_id) if self.author_id else None

        return {
            'id': self.id,
            'title': self.title,
            'description': self.description,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'pic': self.pic,
            'interest_tags': self.get_interest_tags(),
            'format_tags': self.get_format_tags(),
            'author_id': self.author_id,
            'author_name': f"{author.first_name} {author.last_name}" if author else None,
            'organization_id': self.organization_id,
            'organization_name': org.title if org else None,
            'type': 'simple',
            'likes': 0
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
                            db.Column('user_id', db.Integer, db.ForeignKey('user.id')),
                            db.Column('post_event_id', db.Integer, db.ForeignKey('post_event.id'))
                            )

user_registered_events = db.Table('user_registered_events',
                                  db.Column('user_id', db.Integer, db.ForeignKey('user.id')),
                                  db.Column('post_event_id', db.Integer, db.ForeignKey('post_event.id'))
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
    """Нормализует метрики так, чтобы сумма была равна 1"""
    if not metrics:
        return {}
    total = sum(metrics.values())
    if total > 0:
        return {k: v / total for k, v in metrics.items()}
    return metrics


def update_user_interests(user, post):
    """Обновляет метрики интересов пользователя на основе лайкнутых постов"""
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
    """Валидация пароля"""
    if len(password) < 6:
        return False, "Пароль должен содержать минимум 6 символов"
    return True, ""


def validate_email(email):
    """Валидация email"""
    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    if not re.match(pattern, email):
        return False, "Неверный формат email"
    return True, ""


# Создание таблиц и начальных данных
with app.app_context():
    try:
        db.drop_all()
        db.create_all()
        print("✅ БАЗА ДАННЫХ ПЕРЕСОЗДАНА!")

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

        # Создаем тестового пользователя
        if User.query.count() == 0:
            user = User(
                email='test@example.com',
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

        # Создаем тестовые организации и посты
        if PostEvent.query.count() == 0 and User.query.count() > 0:
            # Создаем организации
            org1 = Organisation(
                title='IT Community',
                description='Сообщество разработчиков и IT-специалистов',
                owner_id=1,
                status='approved',
                city='Москва',
                tags=json.dumps(['IT', 'программирование', 'технологии'])
            )
            org2 = Organisation(
                title='Art Space',
                description='Пространство для творчества и искусства',
                owner_id=1,
                status='approved',
                city='Москва',
                tags=json.dumps(['искусства', 'творчество', 'дизайн'])
            )
            org3 = Organisation(
                title='Science Hub',
                description='Научное сообщество и исследовательский центр',
                owner_id=1,
                status='approved',
                city='Москва',
                tags=json.dumps(['наука', 'исследования', 'образование'])
            )
            db.session.add_all([org1, org2, org3])
            db.session.commit()

            # Создаем тестовые мероприятия
            # В разделе создания тестовых постов в Flask app замените test_events на:

            # Создаем тестовые мероприятия
            test_events = [
                # IT мероприятия (5 штук)
                {
                    'title': 'Хакатон по веб-разработке',
                    'description': '48-часовой марафон по созданию веб-приложений',
                    'date_time': datetime.now() + timedelta(days=7),
                    'location': 'Москва, ул. Тверская, 15',
                    'event_type': 'хакатон',
                    'interest_tags': ['IT', 'программирование', 'технологии', 'веб-разработка'],
                    'format_tags': ['офлайн'],
                    'organization_id': org1.id
                },
                {
                    'title': 'Онлайн курс по Python',
                    'description': 'Изучите Python с нуля до продвинутого уровня',
                    'date_time': datetime.now() + timedelta(days=3),
                    'location': 'Онлайн',
                    'event_type': 'лекция',
                    'interest_tags': ['IT', 'программирование', 'Python'],
                    'format_tags': ['онлайн'],
                    'organization_id': org1.id
                },
                {
                    'title': 'AI Conference 2024',
                    'description': 'Конференция по искусственному интеллекту и машинному обучению',
                    'date_time': datetime.now() + timedelta(days=15),
                    'location': 'Москва, Крокус Сити',
                    'event_type': 'конференция',
                    'interest_tags': ['IT', 'искусственный интеллект', 'технологии', 'машинное обучение'],
                    'format_tags': ['гибрид'],
                    'organization_id': org1.id
                },
                {
                    'title': 'Воркшоп по мобильной разработке',
                    'description': 'Практическое занятие по созданию мобильных приложений',
                    'date_time': datetime.now() + timedelta(days=10),
                    'location': 'Москва, ул. Ленинградская, 25',
                    'event_type': 'воркшоп',
                    'interest_tags': ['IT', 'мобильная разработка', 'Android', 'iOS'],
                    'format_tags': ['офлайн'],
                    'organization_id': org1.id
                },
                {
                    'title': 'DevOps Meetup',
                    'description': 'Встреча разработчиков для обсуждения DevOps практик',
                    'date_time': datetime.now() + timedelta(days=5),
                    'location': 'Москва, БЦ Оружейный',
                    'event_type': 'встреча',
                    'interest_tags': ['IT', 'DevOps', 'инфраструктура', 'автоматизация'],
                    'format_tags': ['офлайн'],
                    'organization_id': org1.id
                },

                # Творческие мероприятия (5 штук)
                {
                    'title': 'Онлайн курс по цифровому искусству',
                    'description': 'Изучите основы цифрового искусства и дизайна',
                    'date_time': datetime.now() + timedelta(days=3),
                    'location': 'Онлайн',
                    'event_type': 'мастер-класс',
                    'interest_tags': ['искусства', 'творчество', 'дизайн', 'цифровое искусство'],
                    'format_tags': ['онлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Воркшоп по UI/UX дизайну',
                    'description': 'Практический воркшоп по созданию интерфейсов',
                    'date_time': datetime.now() + timedelta(days=10),
                    'location': 'Москва, ул. Арбат, 25',
                    'event_type': 'воркшоп',
                    'interest_tags': ['дизайн', 'творчество', 'UI/UX', 'IT'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Фотопрогулка по историческому центру',
                    'description': 'Совместная фотосессия и обучение основам фотографии',
                    'date_time': datetime.now() + timedelta(days=8),
                    'location': 'Москва, Красная площадь',
                    'event_type': 'прогулка',
                    'interest_tags': ['фотография', 'творчество', 'искусства', 'путешествия'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Курс по графическому дизайну',
                    'description': 'Освойте Adobe Illustrator и Photoshop с нуля',
                    'date_time': datetime.now() + timedelta(days=12),
                    'location': 'Онлайн',
                    'event_type': 'курс',
                    'interest_tags': ['дизайн', 'графика', 'творчество', 'искусства'],
                    'format_tags': ['онлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Выставка современного искусства',
                    'description': 'Экспозиция работ молодых российских художников',
                    'date_time': datetime.now() + timedelta(days=6),
                    'location': 'Москва, Галерея современного искусства',
                    'event_type': 'выставка',
                    'interest_tags': ['искусства', 'культура', 'творчество', 'живопись'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },

                # Научные мероприятия (5 штук)
                {
                    'title': 'Лекция по квантовой физике',
                    'description': 'Введение в основы квантовой механики для начинающих',
                    'date_time': datetime.now() + timedelta(days=9),
                    'location': 'Москва, МГУ, физический факультет',
                    'event_type': 'лекция',
                    'interest_tags': ['наука', 'физика', 'образование', 'исследования'],
                    'format_tags': ['офлайн'],
                    'organization_id': org3.id
                },
                {
                    'title': 'Научный семинар по биотехнологиям',
                    'description': 'Обсуждение последних достижений в области биотехнологий',
                    'date_time': datetime.now() + timedelta(days=11),
                    'location': 'Онлайн',
                    'event_type': 'семинар',
                    'interest_tags': ['наука', 'биотехнологии', 'медицина', 'исследования'],
                    'format_tags': ['онлайн'],
                    'organization_id': org3.id
                },
                {
                    'title': 'Астрономические наблюдения',
                    'description': 'Ночные наблюдения за звездами и планетами',
                    'date_time': datetime.now() + timedelta(days=14),
                    'location': 'Московская область, астрономическая обсерватория',
                    'event_type': 'наблюдения',
                    'interest_tags': ['наука', 'астрономия', 'космос', 'исследования'],
                    'format_tags': ['офлайн'],
                    'organization_id': org3.id
                },
                {
                    'title': 'Химический эксперимент-шоу',
                    'description': 'Зрелищные химические опыты и объяснения',
                    'date_time': datetime.now() + timedelta(days=7),
                    'location': 'Москва, Парк Горького',
                    'event_type': 'шоу',
                    'interest_tags': ['наука', 'химия', 'образование', 'эксперименты'],
                    'format_tags': ['офлайн'],
                    'organization_id': org3.id
                },
                {
                    'title': 'Онлайн курс по нейробиологии',
                    'description': 'Изучение работы мозга и нервной системы',
                    'date_time': datetime.now() + timedelta(days=4),
                    'location': 'Онлайн',
                    'event_type': 'курс',
                    'interest_tags': ['наука', 'биология', 'медицина', 'нейробиология'],
                    'format_tags': ['онлайн'],
                    'organization_id': org3.id
                },

                # Спортивные мероприятия (5 штук)
                {
                    'title': 'Мастер-класс по йоге',
                    'description': 'Основы йоги для начинающих и продвинутых',
                    'date_time': datetime.now() + timedelta(days=5),
                    'location': 'Москва, студия йоги "Ом"',
                    'event_type': 'мастер-класс',
                    'interest_tags': ['спорт', 'йога', 'здоровье', 'медитация'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Турнир по настольному теннису',
                    'description': 'Еженедельные соревнования для любителей',
                    'date_time': datetime.now() + timedelta(days=6),
                    'location': 'Москва, спортивный клуб "Ракетка"',
                    'event_type': 'турнир',
                    'interest_tags': ['спорт', 'теннис', 'соревнования', 'активный отдых'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Онлайн марафон по бегу',
                    'description': 'Виртуальный марафон с отслеживанием результатов',
                    'date_time': datetime.now() + timedelta(days=10),
                    'location': 'Онлайн',
                    'event_type': 'марафон',
                    'interest_tags': ['спорт', 'бег', 'здоровье', 'активный образ жизни'],
                    'format_tags': ['онлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Воркшоп по функциональному тренингу',
                    'description': 'Интенсивная тренировка для всего тела',
                    'date_time': datetime.now() + timedelta(days=8),
                    'location': 'Москва, фитнес-центр "Энергия"',
                    'event_type': 'воркшоп',
                    'interest_tags': ['спорт', 'фитнес', 'тренировки', 'здоровье'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Велосипедная экскурсия по паркам',
                    'description': 'Групповая велопрогулка с гидом',
                    'date_time': datetime.now() + timedelta(days=12),
                    'location': 'Москва, парк Сокольники',
                    'event_type': 'экскурсия',
                    'interest_tags': ['спорт', 'велоспорт', 'активный отдых', 'путешествия'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },

                # Бизнес и карьера (5 штук)
                {
                    'title': 'Семинар по финансовой грамотности',
                    'description': 'Основы управления личными финансами и инвестирования',
                    'date_time': datetime.now() + timedelta(days=7),
                    'location': 'Москва, бизнес-центр "Сити"',
                    'event_type': 'семинар',
                    'interest_tags': ['бизнес', 'финансы', 'инвестиции', 'экономика'],
                    'format_tags': ['офлайн'],
                    'organization_id': org1.id
                },
                {
                    'title': 'Карьерный воркшоп для IT-специалистов',
                    'description': 'Построение карьеры в IT-индустрии',
                    'date_time': datetime.now() + timedelta(days=9),
                    'location': 'Онлайн',
                    'event_type': 'воркшоп',
                    'interest_tags': ['карьера', 'IT', 'бизнес', 'развитие'],
                    'format_tags': ['онлайн'],
                    'organization_id': org1.id
                },
                {
                    'title': 'Нетворкинг для предпринимателей',
                    'description': 'Вечер знакомств и обмена опытом',
                    'date_time': datetime.now() + timedelta(days=11),
                    'location': 'Москва, коворкинг "Старт"',
                    'event_type': 'встреча',
                    'interest_tags': ['бизнес', 'нетворкинг', 'предпринимательство', 'карьера'],
                    'format_tags': ['офлайн'],
                    'organization_id': org1.id
                },
                {
                    'title': 'Онлайн курс по маркетингу',
                    'description': 'Стратегии digital-маркетинга для бизнеса',
                    'date_time': datetime.now() + timedelta(days=13),
                    'location': 'Онлайн',
                    'event_type': 'курс',
                    'interest_tags': ['маркетинг', 'бизнес', 'digital', 'реклама'],
                    'format_tags': ['онлайн'],
                    'organization_id': org1.id
                },
                {
                    'title': 'Конференция по стартапам',
                    'description': 'Презентации перспективных стартапов и инвесторов',
                    'date_time': datetime.now() + timedelta(days=16),
                    'location': 'Москва, технопарк "Сколково"',
                    'event_type': 'конференция',
                    'interest_tags': ['стартапы', 'бизнес', 'инновации', 'инвестиции'],
                    'format_tags': ['гибрид'],
                    'organization_id': org1.id
                },

                # Языки и культура (5 штук)
                {
                    'title': 'Разговорный клуб английского языка',
                    'description': 'Практика английского в неформальной обстановке',
                    'date_time': datetime.now() + timedelta(days=6),
                    'location': 'Москва, антикафе "Циферблат"',
                    'event_type': 'встреча',
                    'interest_tags': ['языки', 'английский', 'образование', 'культура'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Онлайн курс испанского языка',
                    'description': 'Изучение испанского с носителем языка',
                    'date_time': datetime.now() + timedelta(days=8),
                    'location': 'Онлайн',
                    'event_type': 'курс',
                    'interest_tags': ['языки', 'испанский', 'образование', 'культура'],
                    'format_tags': ['онлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Литературный вечер',
                    'description': 'Чтение и обсуждение современной поэзии',
                    'date_time': datetime.now() + timedelta(days=10),
                    'location': 'Москва, библиотека им. Тургенева',
                    'event_type': 'вечер',
                    'interest_tags': ['литература', 'культура', 'искусства', 'поэзия'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Курс по японской каллиграфии',
                    'description': 'Искусство красивого письма и медитации',
                    'date_time': datetime.now() + timedelta(days=12),
                    'location': 'Москва, культурный центр "Япония"',
                    'event_type': 'курс',
                    'interest_tags': ['культура', 'искусства', 'Япония', 'каллиграфия'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                },
                {
                    'title': 'Фестиваль уличной еды',
                    'description': 'Гастрономический фестиваль с кухнями разных стран',
                    'date_time': datetime.now() + timedelta(days=14),
                    'location': 'Москва, парк Горького',
                    'event_type': 'фестиваль',
                    'interest_tags': ['культура', 'еда', 'путешествия', 'гастрономия'],
                    'format_tags': ['офлайн'],
                    'organization_id': org2.id
                }
            ]

            for event_data in test_events:
                post = PostEvent(
                    title=event_data['title'],
                    description=event_data['description'],
                    date_time=event_data['date_time'],
                    location=event_data['location'],
                    event_type=event_data['event_type'],
                    organization_id=event_data['organization_id']
                )
                post.set_interest_tags(event_data['interest_tags'])
                post.set_format_tags(event_data['format_tags'])
                db.session.add(post)

        db.session.commit()
        print("✅ НАЧАЛЬНЫЕ ДАННЫЕ СОЗДАНЫ!")

    except Exception as e:
        print(f"❌ Ошибка при создании БД: {e}")
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


@app.route('/api/users/<int:user_id>/complete-profile', methods=['POST'])
@jwt_required()
def complete_profile(user_id):
    try:
        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = User.query.get(user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        data = request.get_json()
        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        # Обновляем данные профиля
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

        user.profile_completed = True
        db.session.commit()

        return jsonify({
            "message": "Профиль успешно обновлен",
            "user": user.to_dict(),
            "next_step": "complete_preferences"  # Следующий шаг - опрос предпочтений
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@app.route('/api/users/<int:user_id>/complete-preferences', methods=['POST'])
@jwt_required()
def complete_preferences(user_id):
    try:
        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = User.query.get(user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        data = request.get_json()
        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        # Обновляем метрики на основе выбора пользователя
        interests = data.get('interests', [])
        formats = data.get('formats', [])
        event_types = data.get('event_types', [])

        # Создаем новые метрики на основе выбора
        interests_metrics = {}
        for interest in interests:
            interests_metrics[interest] = 0.5  # Высокий вес для выбранных интересов

        formats_metrics = {}
        for format in formats:
            formats_metrics[format] = 0.5

        event_type_metrics = {}
        for event_type in event_types:
            event_type_metrics[event_type] = 0.5

        # Нормализуем метрики
        interests_metrics = normalize_metrics(interests_metrics)
        formats_metrics = normalize_metrics(formats_metrics)
        event_type_metrics = normalize_metrics(event_type_metrics)

        user.set_interests_metrics(interests_metrics)
        user.set_format_metrics(formats_metrics)
        user.set_event_type_metrics(event_type_metrics)
        user.preferences_completed = True

        # Начисляем достижение за завершение опроса
        registration_achievement = Achievement.query.filter_by(name='Регистрация на платформе').first()
        if registration_achievement:
            user.achievements.append(registration_achievement)
            user.exp += registration_achievement.points

        db.session.commit()

        return jsonify({
            "message": "Предпочтения успешно сохранены",
            "user": user.to_dict(),
            "next_step": "main"  # Переход на главный экран
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@app.route('/api/preferences/categories', methods=['GET'])
def get_preference_categories():
    """Получение категорий для опроса предпочтений"""
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
        user = User.query.get(user_id)
        event = PostEvent.query.get(event_id)

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
    """Получение информации об организации"""
    organisation = Organisation.query.get(org_id)
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
        user = User.query.get(user_id)
        organisation = Organisation.query.get(org_id)

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

            # Определяем следующий шаг для пользователя
            next_step = "main"
            if not user.profile_completed:
                next_step = "complete_profile"
            elif not user.preferences_completed:
                next_step = "complete_preferences"

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
# @jwt_required()
def get_recommended_feed():
    try:
        # Временно используем первого пользователя вместо JWT
        user = User.query.first()
        if not user:
            print("DEBUG: No users found in database")
            return jsonify({"error": "Пользователь не найден"}), 404

        print(f"DEBUG: Using test user: {user.email} (ID: {user.id})")

        # Обрабатываем параметры
        data = request.args or {}

        try:
            limit = int(data.get('limit', 10))
            offset = int(data.get('offset', 0))
        except (ValueError, TypeError):
            limit = 10
            offset = 0

        print(f"DEBUG: Запрос ленты")
        print(f"DEBUG: Метод: {request.method}")
        print(f"DEBUG: Limit: {limit}, Offset: {offset}")

        # Получаем все посты
        event_posts = PostEvent.query.all()
        simple_posts = PostSimple.query.all()
        all_posts = list(event_posts) + list(simple_posts)

        print(f"DEBUG: Найдено постов: {len(all_posts)}")

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

        # Сортируем по релевантности
        scored_posts = []
        for post in all_posts:
            try:
                score = post.calculate_relevance_score(user)
                print(f"DEBUG: Пост '{post.title}' - релевантность: {score}")
                scored_posts.append((post, score))
            except Exception as e:
                print(f"WARNING: Ошибка расчета релевантности для поста {post.id}: {e}")
                scored_posts.append((post, 0.1))

        # Сортируем по убыванию релевантности
        scored_posts.sort(key=lambda x: x[1], reverse=True)

        print(f"DEBUG: После сортировки - топ-3:")
        for i, (post, score) in enumerate(scored_posts[:3]):
            print(f"DEBUG:   {i + 1}. '{post.title}' - {score}")

        # Применяем пагинацию - возвращаем посты в пределах доступного диапазона
        total_posts = len(scored_posts)
        start_idx = min(offset, total_posts)
        end_idx = min(offset + limit, total_posts)

        paginated_posts = scored_posts[start_idx:end_idx]

        print(f"DEBUG: Пагинация: {start_idx}-{end_idx} из {total_posts}")

        # Формируем ответ
        feed_posts = []
        for post, score in paginated_posts:
            post_data = post.to_dict()
            post_data['relevance_score'] = round(score, 3)
            feed_posts.append(post_data)

        print(f"DEBUG: Возвращаем {len(feed_posts)} постов")

        response_data = {
            "posts": feed_posts,
            "count": len(feed_posts),
            "total": total_posts,
            "offset": offset,
            "limit": limit,
            "has_more": end_idx < total_posts  # Добавляем флаг наличия дополнительных постов
        }

        print(f"DEBUG: Final response: {len(feed_posts)} posts, has_more: {end_idx < total_posts}")
        return jsonify(response_data), 200

    except Exception as e:
        print(f"ERROR: Ошибка в get_recommended_feed: {e}")
        import traceback
        traceback.print_exc()

        return jsonify({"error": str(e)}), 500


@app.route('/api/posts/like', methods=['POST'])
@jwt_required()
def like_post_with_interests():
    try:
        user_id = get_jwt_identity()
        user = User.query.get(user_id)
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

        user = User.query.get(user_id)
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
    """Временный эндпоинт для отладки ленты"""
    user_id = get_jwt_identity()
    user = User.query.get(user_id)

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
        user = User.query.get(user_id)

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
        user = User.query.get(user_id)
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
    """Проверка всех данных в базе"""
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

if __name__ == '__main__':
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