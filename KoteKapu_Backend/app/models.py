from .extensions import db, bcrypt
import json
from datetime import datetime, timedelta
from .extensions import db
from . import utils

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
        metrics['preferred_categories'] = utils.normalize_metrics(metrics['preferred_categories'])
        metrics['preferred_formats'] = utils.normalize_metrics(metrics['preferred_formats'])
        metrics['preferred_event_types'] = utils.normalize_metrics(metrics['preferred_event_types'])

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



user_achievements = db.Table(
    'user_achievements',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id')),
    db.Column('achievement_id', db.Integer, db.ForeignKey('achievement.id'))
)

user_subscriptions = db.Table(
    'user_subscriptions',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id')),
    db.Column('organization_id', db.Integer, db.ForeignKey('organisation.id'))
)

user_liked_posts = db.Table(
    'user_liked_posts',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id'), primary_key=True),
    db.Column('post_event_id', db.Integer, db.ForeignKey('post_event.id'), primary_key=True)
)

user_registered_events = db.Table(
    'user_registered_events',
    db.Column('user_id', db.Integer, db.ForeignKey('user.id'), primary_key=True),
    db.Column('post_event_id', db.Integer, db.ForeignKey('post_event.id'), primary_key=True)
)

# --- Определяем связи ---
User.achievements = db.relationship('Achievement', secondary=user_achievements, backref='users')
User.subscriptions = db.relationship('Organisation', secondary=user_subscriptions, backref='subscribers')
User.liked_event_posts = db.relationship('PostEvent', secondary=user_liked_posts, backref='liked_by')
User.registered_events = db.relationship('PostEvent', secondary=user_registered_events, backref='registered_users')
User.user_posts = db.relationship('PostSimple', backref='author', lazy=True)
User.user_organisations = db.relationship('Organisation', backref='owner', lazy=True, foreign_keys='Organisation.owner_id')
Organisation.event_posts = db.relationship('PostEvent', backref='organization', lazy=True)
Organisation.simple_posts = db.relationship('PostSimple', backref='organization', lazy=True)
