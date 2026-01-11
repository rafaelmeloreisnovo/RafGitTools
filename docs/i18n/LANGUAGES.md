# Supported Languages - RafGitTools

## ✅ Current Status

RafGitTools currently supports **3 languages** for UI strings:

| Language | Code | Status |
|----------|------|--------|
| 🇺🇸 English | en | ✅ Complete (Default) |
| 🇧🇷 Portuguese (Brazil) | pt-BR | ✅ Complete |
| 🇪🇸 Spanish | es | ✅ Complete |

### Implementation Details

- **50+ strings** externalized per language
- **Runtime language switching** supported
- **Persistent language preference** using DataStore
- **Material Design 3** language selector dialog

### Resource Files

```
app/src/main/res/
├── values/strings.xml          # English (default)
├── values-pt-rBR/strings.xml   # Portuguese (Brazil)
└── values-es/strings.xml       # Spanish
```

---

## 📋 Documentation Languages

| Document Type | Languages |
|--------------|-----------|
| Core Documentation | English |
| PR Guides | English + Portuguese |
| Code Comments | English |

---

## 🗺️ Future Language Goals

These are **aspirational targets** for future releases:

### Tier 1 (High Priority)
- Chinese (Simplified) - zh-CN
- German - de
- French - fr
- Japanese - ja

### Tier 2 (Medium Priority)
- Korean - ko
- Russian - ru
- Italian - it
- Turkish - tr

### Tier 3 (Community Interest)
- Hindi, Arabic, Polish, Dutch, and more

---

## 🤝 Contributing Translations

Want to help translate RafGitTools?

1. See [Translation Guide](TRANSLATION_GUIDE.md) for instructions
2. Fork the repository
3. Add string resources in `values-{locale}/strings.xml`
4. Submit a Pull Request

---

**Current Language Count**: 3  
**Target Language Count**: 10+ (future)  
**Last Updated**: January 2026
