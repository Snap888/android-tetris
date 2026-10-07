# Android Tetris

Классическая игра Tetris для Android, написанная на Kotlin с использованием SurfaceView.

## Как собрать APK

1. Клонируйте репозиторий:
```bash
git clone https://github.com/Snap888/android-tetris.git
cd android-tetris
```

2. Откройте проект в **Android Studio** (рекомендуется) или соберите из командной строки:
```bash
./gradlew assembleDebug
```

Готовый APK будет здесь:
`app/build/outputs/apk/debug/app-debug.apk`

Для release-сборки:
```bash
./gradlew assembleRelease
```

## Управление
- **Свайп влево / вправо** — движение фигуры
- **Свайп вверх** — поворот
- **Свайп вниз** — ускоренное падение (soft drop)
- **Долгий тап / двойной тап** — hard drop (мгновенное падение)

## Особенности
- Все 7 классических тетромино (I, O, T, S, Z, J, L)
- Поворот с базовой проверкой коллизий
- Очистка линий + система очков
- Увеличение скорости с уровнем
- Отображение следующей фигуры
- Game Over + рестарт по тапу

Приятной игры! 🎮
