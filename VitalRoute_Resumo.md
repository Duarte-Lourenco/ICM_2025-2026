# VitalRoute — Resumo da Aplicação

**Plataforma:** Android (Kotlin + Jetpack Compose)  
**Package:** `com.studio.vitalroute`  
**Min SDK:** 24 | **Target SDK:** 36  
**Arquitetura:** MVVM (ViewModel + StateFlow + collectAsStateWithLifecycle)

---

## Autenticação

- Ecrã de login/registo com email e password (Firebase Authentication)
- Toggle entre modo "Entrar" e "Criar conta" no mesmo ecrã
- Erros do Firebase traduzidos para português
- Ao fazer login, o utilizador entra diretamente na app
- Ao fazer logout (em Definições → "Terminar Sessão"), volta ao ecrã de login
- O estado de autenticação é monitorizado em tempo real via `AuthStateListener`

---

## Abas da Aplicação

### Início (HomeScreen)
- Saudação dinâmica consoante a hora do dia (Bom dia / Boa tarde / Boa noite)
- Nome do utilizador carregado do Firebase Authentication
- Indicadores de GPS e bateria
- Card de estado do sistema ("PRONTO PARA ARRANCAR")
- **Estatísticas semanais reais** carregadas do Firestore:
  - Quilómetros percorridos na semana
  - Tempo ativo na semana
  - Número de incidentes
  - Barra de progresso para objetivo semanal (100 km)
- **Última atividade** com métricas reais (distância, duração, velocidade, elevação)
- Card de sensores (GPS, Bateria, Acelerómetro) com indicadores de estado

---

### Mapas (MapsScreen)
- Mapa interativo usando **OSMDroid** (OpenStreetMap, sem API key)
- **Duas camadas de mapa** selecionáveis por chips no topo:
  - Normal (OpenStreetMap padrão)
  - Ciclismo (CyclOSM — mostra ciclovias, trilhos e infra-estrutura ciclável)
- **Card de meteorologia** em tempo real no canto superior direito:
  - Temperatura, condição do tempo, humidade, vento
  - Dados da **Open-Meteo API** (gratuita, sem API key)
- **Ponto azul de localização** do dispositivo no mapa (MyLocationOverlay)
- **Botão FAB** para centrar o mapa na localização atual
- Suporte a multi-touch (zoom com dois dedos, arrastar)
- Fallback para Aveiro se permissão de localização não estiver concedida

---

### Gravação (RecordingScreen)
- Ecrã de gravação de atividade com cronómetro em tempo real
- **Cronómetro** usando Kotlin Coroutines (`viewModelScope.launch + delay(1000L)`)
- **Grelha 2×2 de métricas:**
  - Tempo de atividade (laranja)
  - Distância em km (branco)
  - Velocidade em km/h (verde)
  - Elevação em metros (azul)
  - Calorias (laranja queimado)
- Botão **INICIAR / PARAR GRAVAÇÃO** com ícones e cores distintas
- Card informativo sobre deteção de queda (visível quando parado)
- **SOS Slider** para acionar emergência manualmente
- **Ao parar a gravação**, a atividade é automaticamente guardada no Firestore (se durou ≥ 10 segundos)

---

### Segurança (SecurityScreen)
- Card de estado de proteção (número de contactos ativos + contagem SOS)
- **Contactos de confiança** carregados e persistidos no Firestore em tempo real:
  - Nome, relação e telefone
  - Badges coloridos: SOS (vermelho) e ZONAS (verde)
  - Botão "Adicionar Contacto"
- **Slider de sensibilidade** de deteção de queda (Baixa / Média / Alta) com badge dinâmico
- **Zonas seguras** (Casa, Trabalho) com ícones em caixas coloridas
- **Alertas automáticos** com Switch (guardados no Firestore):
  - Alerta de imobilidade
  - Notificação de chegada a zona segura
  - Desvio de rota

---

### Diário (DiaryScreen)
- **Resumo do mês atual** calculado a partir das atividades reais no Firestore:
  - Distância total, elevação total, tempo ativo, incidentes
- **Recordes pessoais** calculados automaticamente:
  - Maior distância, velocidade máxima, maior duração, maior elevação
- **Histórico de atividades** real com cards individuais mostrando:
  - Tipo (Ciclismo / Corrida) com ícone
  - Data e período do dia
  - 4 métricas: distância, duração, velocidade, elevação
- Estado vazio com mensagem quando não há atividades registadas
- Botão de acesso às Definições no cabeçalho

---

## Base de Dados — Firebase Firestore

### Estrutura de dados

```
users/
  {uid}/
    ├── (documento raiz)     → name, email
    ├── activities/
    │     {activityId}/      → id, type, startTime, endTime,
    │                           distanceKm, durationSeconds,
    │                           avgSpeedKmh, elevationM, calories
    ├── contacts/
    │     {contactId}/       → id, name, relation, phone,
    │                           sosEnabled, zonesEnabled
    └── settings/
          main/              → fallSensitivity, sosCountdownSecs,
                               immobilityAlertEnabled, immobilityMinutes,
                               arrivalAlertEnabled, routeDeviationEnabled
```

### Regras de segurança
- Cada utilizador só consegue ler/escrever os seus próprios dados
- Autenticação obrigatória para qualquer operação

### Padrões utilizados
- **Flows em tempo real** (`callbackFlow`) para contactos, definições e atividades — a UI atualiza automaticamente quando há alterações no Firestore
- **`suspend` + `await()`** para operações pontuais (guardar atividade, guardar perfil)
- Erros silenciosos quando o utilizador não está autenticado

---

## APIs Externas

| API | Uso | Autenticação |
|---|---|---|
| Open-Meteo | Temperatura, condição, humidade, vento | Nenhuma (gratuita) |
| Overpass API | Consulta de ciclovias OSM (código disponível, botão removido da UI) | Nenhuma (gratuita) |
| CyclOSM Tiles | Camada de mapa especializada em ciclismo | Nenhuma (gratuita) |
| Firebase Auth | Login / Registo / Logout | google-services.json |
| Firebase Firestore | Base de dados em tempo real | google-services.json |

---

## Dependências Principais

| Biblioteca | Versão | Função |
|---|---|---|
| Jetpack Compose + Material 3 | BOM 2024 | UI declarativa |
| Navigation Compose | 2.7.7 | Navegação entre abas |
| Lifecycle ViewModel Compose | 2.8.0 | MVVM |
| OSMDroid | 6.1.18 | Mapa interativo |
| Retrofit + Gson | 2.9.0 | Chamadas HTTP (Open-Meteo, Overpass) |
| Kotlinx Coroutines | 1.7.3 | Assincronismo (timer, APIs, Firestore) |
| Firebase BoM | 33.1.0 | Gestão de versões Firebase |
| Firebase Auth KTX | — | Autenticação |
| Firebase Firestore KTX | — | Base de dados |
| Play Services Location | 21.0.1 | GPS do dispositivo |
| Material Icons Extended | 1.6.0 | Ícones adicionais |

---

## Permissões Android (AndroidManifest.xml)

- `INTERNET` — chamadas às APIs e Firebase
- `ACCESS_FINE_LOCATION` — GPS preciso
- `ACCESS_COARSE_LOCATION` — localização aproximada (rede)

---

## Estrutura de Ficheiros (principais)

```
app/src/main/java/com/studio/vitalroute/
├── MainActivity.kt                        ← gate de autenticação
├── data/
│   ├── api/
│   │   ├── WeatherApiService.kt           ← Open-Meteo (Retrofit)
│   │   ├── WeatherRepository.kt           ← lógica meteo
│   │   └── OverpassApiService.kt          ← Overpass (Retrofit)
│   ├── firebase/
│   │   └── FirestoreRepository.kt         ← todas as ops. Firestore
│   └── model/
│       └── Models.kt                      ← Activity, FirestoreContact,
│                                             UserSettings, UserProfile
├── navigation/
│   └── VitalRouteNavGraph.kt              ← navegação + bottom bar
└── ui/
    ├── auth/
    │   ├── AuthViewModel.kt
    │   └── LoginScreen.kt
    ├── home/
    │   ├── HomeViewModel.kt
    │   └── HomeScreen.kt
    ├── maps/
    │   ├── MapsViewModel.kt
    │   └── MapsScreen.kt
    ├── recording/
    │   ├── RecordingViewModel.kt
    │   └── RecordingScreen.kt
    ├── security/
    │   ├── SecurityViewModel.kt
    │   └── SecurityScreen.kt
    ├── diary/
    │   ├── DiaryViewModel.kt
    │   └── DiaryScreen.kt
    ├── settings/
    │   ├── SettingsViewModel.kt
    │   └── SettingsScreen.kt
    ├── components/
    │   ├── CommonComponents.kt
    │   ├── SectionHeader.kt
    │   └── SosSlider.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```
