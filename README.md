# 🍽️ CheffsKiss

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="CheffsKiss Logo" width="100"/>
</p>

<p align="center">
  <strong>Tu asistente de cocina inteligente para Android</strong><br/>
  Gestiona recetas, planifica tus comidas y cocina con modo inmersivo paso a paso.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-✓-4285F4?logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-✓-FFCA28?logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/Architecture-Hexagonal-blueviolet" />
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue" />
</p>

---

## ✨ Características principales

| Funcionalidad | Descripción |
|---|---|
| 📖 **Explorador de recetas** | Busca recetas por título o por ingredientes disponibles |
| ➕ **Crea tus recetas** | Editor completo con pasos, ingredientes, fotos y duración |
| 🎯 **Focus Mode** | Modo inmersivo paso a paso con temporizador, swipe y pantalla activa |
| 📅 **Meal Planner** | Planifica tus comidas semanales por tipo de comida y día |
| 📚 **Colecciones** | Organiza tus recetas favoritas en listas personalizadas |
| 👤 **Perfiles sociales** | Sigue a otros cocineros y descubre sus recetas |
| 🔐 **Autenticación** | Registro e inicio de sesión seguros con Firebase Auth |

---

## 📱 Capturas de pantalla

> *Pantalla de inicio · Detalle de receta · Focus Mode · Meal Planner*

---

## 🏗️ Arquitectura

CheffsKiss está construida sobre **Arquitectura Hexagonal (Ports & Adapters)** con separación estricta en tres capas:

```
┌─────────────────────────────────────────┐
│              UI Layer                   │
│   Jetpack Compose · ViewModels · Nav    │
├─────────────────────────────────────────┤
│           Application Layer             │
│   Commands · Queries · Services · Ports │
├─────────────────────────────────────────┤
│             Domain Layer                │
│   Recipe · MealPlan · User · Step       │
├─────────────────────────────────────────┤
│          Infrastructure Layer           │
│   Firebase · Room · Retrofit · Coil     │
└─────────────────────────────────────────┘
```

### Patrones aplicados

- **CQRS** — Separación de comandos (`CreateRecipeCommand`, `DeleteMealPlanCommand`…) y consultas (`GetAllRecipesQuery`, `GetMealPlansQuery`…)
- **Repository Pattern** — Puertos de salida para `RecipeRepository`, `MealPlanRepository`, `RecipeCollectionRepository`
- **Command Pattern** — Cada acción de usuario es un `Command` ejecutable y testeable de forma aislada
- **ViewModel + StateFlow** — Estado de UI reactivo con Kotlin Coroutines

---

## 🧰 Stack tecnológico

| Área | Tecnología |
|---|---|
| **Lenguaje** | Kotlin 2.x |
| **UI** | Jetpack Compose + Material 3 |
| **Navegación** | Navigation Compose |
| **Backend / BaaS** | Firebase Firestore, Firebase Auth, Firebase Storage |
| **Imágenes** | Coil + API de fotos externa |
| **Coroutines** | Kotlin Coroutines + Flow |
| **Build** | Gradle Version Catalogs (`libs.versions.toml`) |
| **DI** | Manual (puertos e inyección por constructor) |
| **Persistencia local** | DataStore / Room (sesiones Focus) |

---

## 🚀 Primeros pasos

### Requisitos previos

- Android Studio Hedgehog o superior
- JDK 17+
- Cuenta de Firebase con un proyecto configurado
- Android SDK 26+

### 1. Clona el repositorio

```bash
git clone https://github.com/tu-usuario/cheffskiss.git
cd cheffskiss
```

### 2. Configura Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com)
2. Habilita **Authentication** (Email/Password), **Firestore** y **Storage**
3. Descarga el archivo `google-services.json` y colócalo en `app/`

### 3. Configura la API de fotos (opcional)

Añade en tu archivo `local.properties` (no se sube al repositorio):

```properties
recipe.photo.api.key=TU_CLAVE_AQUI
recipe.photo.base.url=https://plytrox.com/photos
```

### 4. Compila y ejecuta

```bash
./gradlew assembleDebug
```

O abre el proyecto en **Android Studio** y pulsa ▶️ Run.

---

## 📂 Estructura del proyecto

```
app/
└── src/main/java/software/ulpgc/cheffskiss/
    ├── application/
    │   ├── control/       # Comandos (CreateRecipeCommand, DeleteMealPlanCommand…)
    │   ├── port/          # Interfaces de puertos (RecipeRepository, Authenticator…)
    │   └── services/      # Queries y servicios de aplicación
    ├── domain/
    │   ├── model/         # Entidades: Recipe, MealPlan, Step, User…
    │   ├── store/         # Interfaces de stores
    │   └── vo/            # Value Objects: Username, Description…
    ├── infrastructure/
    │   └── adapter/
    │       ├── input/     # Adaptadores de entrada (Firebase readers)
    │       └── output/    # Adaptadores de salida (Firebase services, Storage)
    └── ui/
        ├── screen/        # Pantallas Compose (Home, Explore, MealPlan, Focus…)
        ├── components/    # Componentes reutilizables
        ├── navigation/    # Grafos de navegación
        └── theme/         # Colores, tipografía y tema Material 3
```

---

## 🎯 Focus Mode

El **Focus Mode** es la funcionalidad estrella de CheffsKiss. Permite cocinar recetas paso a paso con:

- 🔒 **Pantalla activa** mientras cocinas
- ⏱️ **Temporizador circular** por paso con avance automático opcional
- 👆 **Swipe** entre pasos
- 📋 **Lista de ingredientes** verificable antes de empezar
- 💾 **Guardado de progreso** para retomar más tarde
- 🔡 **Modo texto grande** para leer desde lejos

---

## 🤝 Contribuir

Las contribuciones son bienvenidas. Por favor:

1. Haz un fork del proyecto
2. Crea una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Haz commit de tus cambios (`git commit -m 'feat: añade nueva funcionalidad'`)
4. Haz push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

---

## 📄 Licencia

Este proyecto está bajo la licencia **Apache 2.0**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

<p align="center">
  Hecho con ❤️ y Jetpack Compose · Universidad de Las Palmas de Gran Canaria
</p>
