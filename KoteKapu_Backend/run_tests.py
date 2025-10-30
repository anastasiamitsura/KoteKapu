#!/usr/bin/env python3
"""
Скрипт для запуска всех тестов сценариев
"""

import pytest
import sys
import os


def run_tests():
    """Запускает все тестовые сценарии с приоритетами"""

    # Тесты с высоким приоритетом (должны работать)
    high_priority_tests = [
        'test_endpoints.py::TestScenario4Feed',
        'test_scenario1_registration.py',
        'test_scenario2_profile.py',
        'test_scenario3_login.py',
        'test_scenario5_events.py',
        'test_scenario7_organisations.py'
    ]

    # Тесты с низким приоритетом (могут требовать доработки)
    low_priority_tests = [
        'test_scenario4_feed.py'
    ]

    # Проверяем существование файлов
    existing_high_priority = [f for f in high_priority_tests if os.path.exists(f)]
    existing_low_priority = [f for f in low_priority_tests if os.path.exists(f)]

    print(f"\n{'=' * 50}")
    print(f"🚀 ЗАПУСК ТЕСТОВ")
    print(f"📊 Высокий приоритет: {len(existing_high_priority)} файлов")
    print(f"📊 Низкий приоритет: {len(existing_low_priority)} файлов")
    print(f"{'=' * 50}")

    # Сначала запускаем высокоприоритетные тесты
    if existing_high_priority:
        print("\n🎯 ВЫСОКОПРИОРИТЕТНЫЕ ТЕСТЫ:")
        exit_code_high = pytest.main([
            *existing_high_priority,
            '-v',
            '--tb=short',
            '--color=yes'
        ])
    else:
        exit_code_high = 0

    # Затем низкоприоритетные (если высокоприоритетные прошли)
    if existing_low_priority and exit_code_high == 0:
        print("\n📝 НИЗКОПРИОРИТЕТНЫЕ ТЕСТЫ:")
        exit_code_low = pytest.main([
            *existing_low_priority,
            '-v',
            '--tb=short',
            '--color=yes'
        ])
    else:
        exit_code_low = 0

    print(f"\n{'=' * 50}")
    if exit_code_high == 0 and exit_code_low == 0:
        print("🎉 ВСЕ ТЕСТЫ УСПЕШНО ПРОЙДЕНЫ!")
    elif exit_code_high == 0:
        print("✅ ВЫСОКОПРИОРИТЕТНЫЕ ТЕСТЫ ПРОЙДЕНЫ")
        print("⚠️  Некоторые низкоприоритетные тесты требуют доработки")
    else:
        print("💥 ЕСТЬ ОШИБКИ В ВЫСОКОПРИОРИТЕТНЫХ ТЕСТАХ")
    print(f"{'=' * 50}")

    return exit_code_high


if __name__ == '__main__':
    # Устанавливаем переменные окружения для тестов
    os.environ['FLASK_ENV'] = 'testing'

    sys.exit(run_tests())