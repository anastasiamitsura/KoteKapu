
"""Utilities extracted from original app.py"""

from .extensions import db, bcrypt

import json
from datetime import datetime, timedelta
import re


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
    if not password or not isinstance(password, str):
        return False, "Пароль должен быть строкой"

    if len(password) < 6:
        return False, "Пароль должен содержать минимум 6 символов"
    return True, ""


def validate_email(email):
    """Валидация email"""
    if not email or not isinstance(email, str):
        return False, "Email должен быть строкой"

    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    if not re.match(pattern, email):
        return False, "Неверный формат email"
    return True, ""

