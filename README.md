# Android Tetris

Классическая игра Tetris для Android, написанная на Kotlin с использованием SurfaceView.

## Скачать готовый APK (самый простой способ)

1. Перейди во вкладку **Actions** репозитория:  
   https://github.com/Snap888/android-tetris/actions
2. Выбери последний успешный workflow **Build APK**
3. Внизу в разделе **Artifacts** скачай файл **Tetris-APK**
4. Распакуй zip и установи `app-debug.apk` на телефон

> Workflow запускается автоматически при каждом пуше в `main`.  
> Также можно запустить вручную: Actions → Build APK → Run workflow.

## Сборка локально

1. Клонируй репозиторий:
```bash
git clone https://github.com/Snap888/android-tetris.git
cd android-tetris
```

2. Открой проект в **Android Studio** или собери из терминала:
```bash
./gradlew assembleDebug
```

Готовый APK: `app/build/outputs/apk/debug/app-debug.apk`

## Управление
- **Свайп влево / вправо** — движение фигуры
- **Свайп вверх** — поворот
- **Свайп вниз** — soft drop
- **Очень длинный свайп вниз** — hard drop
- При Game Over — просто тапни по экрану, чтобы начать заново

## Особенности
- Все 7 классических тетромино
- Поворот + простые wall-kicks
- Очистка линий + система очков
- Рост скорости с уровнем
- Отображение следующей фигуры
- Game Over + рестарт

Приятной игры! 🎮
