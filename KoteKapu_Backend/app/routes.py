
from flask import Blueprint, request, jsonify, current_app
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from .extensions import db, bcrypt
from .models import Achievement, Organisation, User, PostEvent, PostSimple
from . import utils
from . import models
from . import constants
from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity
from datetime import datetime, timedelta
import json
import os
import random
import re

bp = Blueprint('api', __name__)



@bp.route('/')
def home():
    return jsonify({"message": "Flask Auth API работает! 🚀", "status": "running"})


@bp.route('/test')
def test():
    return "✅ СЕРВЕР РАБОТАЕТ!"

@bp.route('/register', methods=['POST'])
def register_simple():
    return jsonify({"status": "success", "message": "Регистрация работает!"})


@bp.route('/api/register', methods=['POST'])
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
        is_valid_email, email_error = utils.validate_email(data['email'])
        if not is_valid_email:
            return jsonify({"error": email_error}), 400

        # Валидация пароля (ТОЛЬКО МИНИМАЛЬНАЯ ДЛИНА)
        password = data['password']
        if len(password) < 6:
            return jsonify({"error": "Пароль должен содержать минимум 6 символов"}), 400

        # ПРОВЕРЯЕМ ТОЛЬКО EMAIL НА УНИКАЛЬНОСТЬ
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
            "next_step": "complete_profile"
        }), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": f"Внутренняя ошибка сервера: {str(e)}"}), 500


@bp.route('/api/users/<int:user_id>/complete-profile', methods=['POST'])
@jwt_required()
def complete_profile(user_id):
    try:
        print("=" * 50)
        print("🚨 DEBUG COMPLETE PROFILE STARTED")
        print(f"🔐 Headers: {dict(request.headers)}")
        print(f"🔐 Auth Header: {request.headers.get('Authorization')}")

        current_user_id = get_jwt_identity()
        print(f"🔐 JWT User: {current_user_id}, Requested: {user_id}")

        data = request.get_json()
        print(f"📦 Data: {data}")
        print(f"📦 Data type: {type(data)}")
        print("🚨 DEBUG COMPLETE PROFILE ENDED")
        print("=" * 50)

        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        # ПРОСТО СОХРАНИ ВСЁ
        user.phone = data.get('phone', '')
        user.age_user = data.get('age_user', 0)
        user.placement = data.get('placement', '')
        user.study_place = data.get('studyPlace', '') or data.get('study_place', '')
        user.grade_course = data.get('gradeCourse', '') or data.get('grade_course', '')

        user.profile_completed = True
        db.session.commit()

        return jsonify({
            "message": "✅ ПРОФИЛЬ ЗАПОЛНЕН!",
            "user": user.to_dict(),
            "next_step": "complete_preferences"
        }), 200

    except Exception as e:
        print(f"💥 CRITICAL ERROR: {e}")
        import traceback
        traceback.print_exc()
        db.session.rollback()
        return jsonify({"error": str(e)}), 500



@bp.route('/api/users/<int:user_id>/complete-preferences', methods=['POST'])
@jwt_required()
def complete_preferences(user_id):
    try:
        print(f"🔐 DEBUG: Complete Preferences Headers: {dict(request.headers)}")
        print(f"🔐 DEBUG: Authorization Header: {request.headers.get('Authorization')}")

        current_user_id = get_jwt_identity()
        print(f"🔐 DEBUG: JWT User ID: {current_user_id}, Requested User ID: {user_id}")

        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        data = request.get_json()
        print(f"📦 DEBUG: Received Preferences Data: {data}")

        if not data:
            return jsonify({"error": "Отсутствуют данные"}), 400

        # ПРОСТО СОХРАНЯЕМ ДАННЫЕ
        interests = data.get('interests', [])
        formats = data.get('formats', [])
        event_types = data.get('eventTypes', [])  # Обрати внимание на eventTypes vs event_types

        user.set_interests_metrics({interest: 1.0 for interest in interests})
        user.set_format_metrics({format_type: 1.0 for format_type in formats})
        user.set_event_type_metrics({event_type: 1.0 for event_type in event_types})
        user.preferences_completed = True

        db.session.commit()

        return jsonify({
            "message": "✅ ПРЕДПОЧТЕНИЯ СОХРАНЕНЫ!",
            "user": user.to_dict(),
            "next_step": "main"
        }), 200

    except Exception as e:
        print(f"💥 ERROR: {e}")
        import traceback
        traceback.print_exc()
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@bp.route('/api/users/<int:user_id>/profile', methods=['GET'])
@jwt_required()
def get_user_profile(user_id):
    try:
        print("🚨 DEBUG: Get profile started")

        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        print(f"🚨 DEBUG: User found: {user.email}")

        # Безопасно получаем статистику
        try:
            stats = {
                'events_attended': len(user.registered_events),
                'events_created': len([post for post in PostEvent.query.all() if
                                       post.organization and post.organization.owner_id == user_id]),
                'organizations_count': len(user.user_organisations),
                'likes_given': len(user.liked_event_posts),
                'exp': user.exp or 0,
                'level': (user.exp or 0) // 100
            }
        except Exception as stats_error:
            print(f"⚠️ WARNING: Error calculating stats: {stats_error}")
            stats = {
                'events_attended': 0,
                'events_created': 0,
                'organizations_count': 0,
                'likes_given': 0,
                'exp': 0,
                'level': 0
            }

        print("🚨 DEBUG: Stats calculated")

        # БЕЗОПАСНО получаем achievements
        try:
            achievements_data = []
            for ach in user.achievements:
                achievements_data.append({
                    'id': ach.id,
                    'name': ach.name,
                    'description': ach.description,
                    'points': ach.points,
                })
        except Exception as ach_error:
            print(f"⚠️ WARNING: Error getting achievements: {ach_error}")
            achievements_data = []

        print("🚨 DEBUG: Achievements processed")

        # БЕЗОПАСНО вызываем to_dict()
        try:
            user_dict = user.to_dict()
            print("🚨 DEBUG: User.to_dict() successful")
        except Exception as dict_error:
            print(f"💥 ERROR in user.to_dict(): {dict_error}")
            # Возвращаем простой вариант
            user_dict = {
                'id': user.id,
                'email': user.email,
                'first_name': user.first_name,
                'last_name': user.last_name,
                'profile_completed': bool(user.profile_completed),
                'preferences_completed': bool(user.preferences_completed)
            }

        response_data = {
            "user": user_dict,
            "stats": stats,
            "achievements": achievements_data
        }

        print("🚨 DEBUG: Sending response")
        return jsonify(response_data), 200

    except Exception as e:
        print(f"💥 CRITICAL ERROR in get_user_profile: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": f"Server error: {str(e)}"}), 500

@bp.route('/api/users/<int:user_id>/profile', methods=['PUT'])
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

@bp.route('/api/users/<int:user_id>/events', methods=['GET'])
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


@bp.route('/api/events/<int:event_id>', methods=['GET'])
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





@bp.route('/api/search', methods=['POST'])
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


@bp.route('/api/search/suggestions', methods=['GET'])
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



@bp.route('/api/organisations/<int:org_id>/events', methods=['POST'])
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


@bp.route('/api/organisations/<int:org_id>/events', methods=['GET'])
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

@bp.route('/api/preferences/categories', methods=['GET'])
def get_preference_categories():
    """Получение категорий для опроса предпочтений"""
    return jsonify({
        "interest_categories": constants.INTEREST_CATEGORIES,
        "format_types": constants.FORMAT_TYPES,
        "event_types": constants.EVENT_TYPES
    }), 200


@bp.route('/api/events/<int:event_id>/register', methods=['POST'])
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
        utils.update_user_interests(user, event)

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


@bp.route('/api/organisations', methods=['POST'])
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


@bp.route('/api/organisations/<int:org_id>', methods=['GET'])
def get_organisation(org_id):
    """Получение информации об организации"""
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


@bp.route('/api/organisations/<int:org_id>/subscribe', methods=['POST'])
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


@bp.route('/api/login', methods=['POST'])
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


@bp.route('/api/feed/recommended', methods=['GET'])
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

@bp.route('/api/debug/user-state/<int:user_id>', methods=['GET'])
@jwt_required()
def debug_user_state(user_id):
    """Отладочный эндпоинт для проверки состояния пользователя"""
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

@bp.route('/api/posts/like', methods=['POST'])
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
        utils.update_user_interests(user, post)

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


@bp.route('/api/users/<int:user_id>/interests', methods=['GET'])
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


@bp.route('/api/debug/feed', methods=['GET'])
@jwt_required()
def debug_feed():
    """Временный эндпоинт для отладки ленты"""
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


@bp.route('/api/feed', methods=['GET'])
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


@bp.route('/api/posts/like/<int:post_id>', methods=['POST'])
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


@bp.route('/api/users', methods=['GET'])
def get_users():
    users = User.query.all()
    return jsonify({
        "users": [user.to_dict() for user in users],
        "count": len(users)
    })


@bp.route('/api/debug/check', methods=['GET'])
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

@bp.route('/api/ping', methods=['GET'])
def ping():
    return jsonify({"message": "pong", "status": "ok"}), 200



@bp.route('/api/debug/db-check', methods=['GET'])
def debug_db_check():
    """Проверка состояния базы данных"""
    try:
        users = User.query.all()

        result = {
            "total_users": len(users),
            "users": []
        }

        for user in users:
            result["users"].append({
                "id": user.id,
                "email": user.email,
                "profile_completed": user.profile_completed,
                "preferences_completed": user.preferences_completed,
                "phone": user.phone,
                "age_user": user.age_user,
                "first_name": user.first_name,
                "last_name": user.last_name
            })

        return jsonify(result), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@bp.route('/api/debug/test-register', methods=['POST'])
def debug_test_register():
    """Тестовый эндпоинт для проверки регистрации"""
    test_data = {
        "email": f"test_{random.randint(1000, 9999)}@example.com",
        "password": "password123",
        "first_name": "Test",
        "last_name": "User"
    }

    print(f"🧪 DEBUG: Test registration with: {test_data}")

    # Имитируем запрос регистрации
    try:
        # Проверяем существование пользователя
        existing_user = User.query.filter_by(email=test_data['email']).first()
        if existing_user:
            return jsonify({"error": "Test user already exists"}), 400

        user = User(
            email=test_data['email'],
            first_name=test_data['first_name'],
            last_name=test_data['last_name']
        )
        user.set_password(test_data['password'])

        # Устанавливаем начальные метрики
        initial_interests = {
            'IT': 0.1, 'искусства': 0.1, 'музыка': 0.1, 'языки': 0.1,
            'экономика': 0.1, 'менеджмент': 0.1, 'творчество': 0.1,
            'спорт': 0.1, 'инжинерия': 0.1, 'культура': 0.1
        }
        user.set_interests_metrics(initial_interests)
        user.set_format_metrics({'онлайн': 0.33, 'офлайн': 0.33, 'гибрид': 0.34})
        user.set_event_type_metrics({})

        db.session.add(user)
        db.session.commit()

        return jsonify({
            "message": "Тестовый пользователь создан",
            "user": user.to_dict()
        }), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": str(e)}), 500


@bp.route('/api/debug/check-json', methods=['POST'])
def debug_check_json():
    """Проверка получения JSON данных"""
    print(f"📦 DEBUG: Headers: {dict(request.headers)}")
    print(f"📦 DEBUG: Content-Type: {request.content_type}")
    print(f"📦 DEBUG: Data: {request.data}")

    if request.is_json:
        data = request.get_json()
        return jsonify({
            "status": "JSON received",
            "data": data,
            "content_type": request.content_type
        }), 200
    else:
        return jsonify({
            "status": "Not JSON",
            "content_type": request.content_type,
            "data": request.data.decode('utf-8') if request.data else None
        }), 400


@bp.route('/api/quick-check/<int:user_id>', methods=['GET'])
def quick_check(user_id):
    user = db.session.get(User, user_id)
    if not user:
        return jsonify({"error": "Пользователь не найден"}), 404

    return jsonify({
        "user_id": user.id,
        "profile_completed": user.profile_completed,
        "preferences_completed": user.preferences_completed,
        "status": "OK"
    })


@bp.route('/api/emergency-fix/<int:user_id>', methods=['POST'])
def emergency_fix(user_id):
    """ЭКСТРЕННОЕ ИСПРАВЛЕНИЕ БЕЗ JWT"""
    try:
        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        user.profile_completed = True
        user.preferences_completed = True
        db.session.commit()

        return jsonify({
            "message": "✅ ЭКСТРЕННОЕ ИСПРАВЛЕНИЕ ВЫПОЛНЕНО!",
            "user_id": user_id,
            "profile_completed": True,
            "preferences_completed": True
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@bp.route('/api/simple-profile/<int:user_id>', methods=['GET'])
@jwt_required()
def simple_profile(user_id):
    """ПРОСТОЙ ПРОФИЛЬ БЕЗ СЛОЖНОЙ ЛОГИКИ"""
    try:
        current_user_id = get_jwt_identity()
        if current_user_id != user_id:
            return jsonify({"error": "Доступ запрещен"}), 403

        user = db.session.get(User, user_id)
        if not user:
            return jsonify({"error": "Пользователь не найден"}), 404

        # САМЫЙ ПРОСТОЙ ОТВЕТ
        return jsonify({
            "user": {
                "id": user.id,
                "email": user.email,
                "first_name": user.first_name,
                "last_name": user.last_name,
                "profile_completed": bool(user.profile_completed),
                "preferences_completed": bool(user.preferences_completed)
            },
            "status": "OK"
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500