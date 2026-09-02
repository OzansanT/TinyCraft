# TinyCraft Assets

Keep runtime assets grouped by capability.

```text
assets/
├── textures/
│   ├── blocks/
│   ├── entities/
│   ├── environment/
│   └── ui/
├── audio/
│   ├── blocks/
│   ├── entities/
│   ├── player/
│   └── ui/
├── fonts/
├── shaders/
└── data/
```

Do not duplicate the same asset under multiple feature folders. When literal asset paths begin repeating, add a centralized `AssetRegistry` in the core module.
