
from .extensions import db
from .models import User, Achievement, Organisation, PostEvent, PostSimple
from datetime import datetime, timedelta
import json
import traceback
import os


def init_db(app):
    db_file = 'app_new.db'
    db_exists = os.path.exists(db_file)

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
                    {'name': 'Регистрация на платформе', 'description': 'Вы зарегистрировались на платформе',
                     'points': 10},
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

                initial_event_types = {'хакатон': 0.3, 'лекция': 0.2, 'мастер-класс': 0.2, 'встреча': 0.1,
                                       'семинар': 0.2}
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
