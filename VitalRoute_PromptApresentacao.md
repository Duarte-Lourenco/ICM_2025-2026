# Prompt para LLM gerador de PowerPoint — VitalRoute

> Copia tudo o que está abaixo da linha "===" e cola na ferramenta de geração de PowerPoint (Gamma, Tome, Slidesgo AI, MagicSlides, GPT, etc.). Substitui apenas os campos `[ENTRE PARÊNTESES]` se a ferramenta os deixar visíveis ou se quiseres ajustar nomes/datas.

===

## INSTRUÇÕES PARA O LLM

Gera uma apresentação em PowerPoint (.pptx) com **13 slides** sobre o projeto **VitalRoute**, uma aplicação Android desenvolvida na cadeira de Introdução à Computação Móvel (ICM) da Universidade de Aveiro, ano letivo 2025/26. A apresentação dura **15 minutos** e será defendida por **dois alunos** num "Projects Summit" académico.

### Estilo visual obrigatório

- **Tema escuro** (background `#0E0E12` ou similar), porque a app é dark-themed.
- **Cor de destaque**: laranja `#FF6B35` (acentos, títulos, ícones).
- **Cores secundárias**: verde `#4ADE80` (sucesso/velocidade), vermelho `#EF4444` (SOS/alerta), azul `#3B82F6` (info/elevação).
- Tipografia: sans-serif moderna (Inter, Roboto ou similar).
- Cada slide deve ter um **título curto e direto** (máx. 6 palavras) no topo.
- **Bullet points curtos** (máx. 8 palavras por linha, máx. 4 bullets por slide).
- **Espaço reservado para uma imagem/screenshot em cada slide técnico** — coloca placeholders `[INSERIR SCREENSHOT: descrição]` à direita do conteúdo.
- Rodapé com "VitalRoute · ICM 2025/26 · UA-DETI" e número da página.
- Não uses emojis nos slides.

### Tom

Profissional, conciso, orientado a engenharia. Evita marketing-speak. Os destinatários são professores de Engenharia Informática, por isso valoriza precisão técnica.

---

## CONTEÚDO DOS 13 SLIDES

### Slide 1 — Capa
- **Título**: VitalRoute
- **Subtítulo**: Fitness + segurança ativa para atletas ao ar livre
- **Logo**: placeholder centrado `[INSERIR LOGO: logo_centrada.png]`
- **Rodapé**: "Introdução à Computação Móvel · 2025/26"
- **Autores**: [NOME DO ALUNO 1] · [NOME DO ALUNO 2]
- **Data**: 19 de maio de 2026
- **Universidade**: Universidade de Aveiro · DETI

### Slide 2 — Problema & Conceito
- **Título**: O problema que resolvemos
- **Bullets**:
  - Ciclistas e corredores treinam sozinhos
  - Acidentes sem testemunhas são frequentes
  - Apps de fitness atuais ignoram segurança
  - Botões de SOS dedicados ignoram fitness
- **Caixa de destaque** (laranja, em baixo): "VitalRoute = fitness tracker + sistema de emergência automático num só app"
- **Imagem**: `[INSERIR IMAGEM: ciclista sozinho em estrada / acidente / split conceptual]`

### Slide 3 — Journey Map do utilizador
- **Título**: Como a Marta usa o VitalRoute
- **Persona breve** (canto superior esquerdo): "Marta, 34, ciclista de fim-de-semana, sai sozinha 2x por semana"
- **Fluxograma horizontal com 6 passos** (cada um com ícone):
  1. Abre app → vê meteorologia e camada de mapa de ciclismo
  2. Confirma contactos de confiança em Segurança
  3. Carrega "Iniciar gravação" → começa percurso
  4. Cai → sensibilidade configurada deteta o impacto
  5. Slider SOS aparece → 10s para cancelar
  6. SMS automático para contactos com localização GPS
- **Rodapé**: "Touchpoint principal: a app. Touchpoints secundários: SMS, contactos do telemóvel, mapas OSM."

### Slide 4 — Tour geral da app
- **Título**: Cinco ecrãs, uma jornada
- **Layout**: 5 mockups de telemóvel lado a lado em linha
- **Imagens**: `[INSERIR 5 SCREENSHOTS: Início | Mapas | Iniciar | Segurança | Diário]`
- **Legendas curtas por baixo de cada um**:
  - Início — dashboard semanal e estado dos sensores
  - Mapas — OSM com camada de ciclismo e meteorologia em tempo real
  - Iniciar — cronómetro, métricas em tempo real e SOS slider
  - Segurança — contactos, sensibilidade de queda, zonas seguras
  - Diário — histórico, recordes pessoais, resumo mensal
- **Nota visual**: destacar a bottom navigation bar laranja na base de cada mockup.

### Slide 5 — Arquitetura geral (handover para apresentador 2)
- **Título**: Arquitetura em camadas (MVVM + UDF)
- **Diagrama central** (3 colunas verticais com setas):
  - **Coluna 1 — UI Layer**: Compose Screens (Home, Maps, Recording, Security, Diary, Auth)
  - **Coluna 2 — State Holders**: ViewModels expõem `StateFlow<UiState>`; UI consome com `collectAsStateWithLifecycle`
  - **Coluna 3 — Data Layer**: FirestoreRepository, WeatherRepository, NominatimRepository, SosManager
- **Setas**: "State flows down ↓ · Events flow up ↑" (a marcar o padrão UDF)
- **Rodapé técnico**: "Padrão recomendado pela Android Architecture Guide"

### Slide 6 — UI: Jetpack Compose & State
- **Título**: UI declarativa com Compose
- **Bullets**:
  - Jetpack Compose + Material 3 (BOM 2024)
  - Composables stateless · state hoisting
  - `ViewModel` + `StateFlow` sobrevive a config changes
  - Tema escuro custom (Theme.kt, Color.kt)
- **Snippet de código** (caixa monospace, fundo cinzento escuro):
```kotlin
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    HomeContent(state, onSosClick = vm::triggerSos)
}
```
- **Imagem**: `[INSERIR SCREENSHOT: HomeScreen]`

### Slide 7 — Camada de dados: Firestore em tempo real
- **Título**: Persistência reativa com Firestore
- **Esquema de coleções** (caixa monospace à esquerda):
```
users/{uid}/
  ├─ (raiz)        → name, email
  ├─ activities/   → distancia, duracao, velocidade, ...
  ├─ contacts/     → nome, telefone, sosEnabled
  └─ settings/main → fallSensitivity, alertas, ...
```
- **Bullets à direita**:
  - `callbackFlow` converte listeners do Firestore em `Flow<T>` Kotlin
  - UI atualiza-se automaticamente quando outro dispositivo escreve
  - Regras de segurança: cada user só lê/escreve os seus dados
  - Autenticação por email/password com Firebase Auth + `AuthStateListener`

### Slide 8 — Navegação & APIs REST externas
- **Título**: Navigation Compose + APIs REST
- **Coluna esquerda (Navegação)**:
  - Navigation Compose v2.7.7
  - 5 destinos principais + Settings, ActivityDetail
  - Bottom bar escondida em ecrãs detalhados
- **Coluna direita (APIs externas via Retrofit + Gson)**:
  - Open-Meteo — temperatura, humidade, vento (sem API key)
  - Nominatim — geocoding reverso (OSM)
  - Overpass — pontos de interesse OSM
- **Rodapé técnico**: "Tudo em coroutines (`suspend` + `viewModelScope`) — zero bloqueios no main thread"

### Slide 9 — Localização & Mapas (handover para apresentador 1)
- **Título**: Localização e Mapas OpenStreetMap
- **Bullets esquerda**:
  - `FusedLocationProviderClient` (Play Services)
  - Permissões pedidas em contexto, nunca no arranque
  - `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`
  - `MyLocationOverlay` mostra ponto azul
- **Bullets direita**:
  - **OSMDroid** em vez de Google Maps
  - Camadas: OSM padrão + **CyclOSM** (ciclovias)
  - Sem API key · open-source · zero billing
  - Fallback para Aveiro sem permissão
- **Imagem**: `[INSERIR SCREENSHOT: MapsScreen com camada CyclOSM]`

### Slide 10 — Gravação em background
- **Título**: Foreground Service para gravar percursos
- **Decision path** (em árvore, à esquerda):
```
Sobrevive a app em background?
 ├─ Sim → Tarefa visível? → Foreground Service ✓
 └─ Não → Coroutine no ViewModelScope
```
- **Bullets à direita**:
  - `RecordingService` com `foregroundServiceType="location"`
  - Notificação persistente enquanto grava
  - Continua com ecrã apagado · não perde percurso
  - Cronómetro via `delay(1000L)` em coroutine
  - Permissão `FOREGROUND_SERVICE_LOCATION` declarada
- **Imagem**: `[INSERIR SCREENSHOT: notificação persistente VitalRoute]`

### Slide 11 — Sistema SOS (Intents + SMS)
- **Título**: Emergência: do sensor ao SMS
- **Fluxo horizontal com 4 etapas**:
  1. Queda detetada pela sensibilidade configurada
  2. `SosSlider` aparece — 10s para cancelar
  3. `SosManager` envia SMS via `SmsManager` para contactos
  4. Mensagem inclui última localização GPS
- **Bullets adicionais**:
  - Picker de contactos via Intent implícito (`ACTION_PICK` em `Contacts.CONTENT_URI`)
  - Exportação de atividades em GPX/CSV via `FileProvider` + `ACTION_SEND`
  - Permissões `SEND_SMS` + `READ_CONTACTS` justificadas em contexto
- **Imagem**: `[INSERIR SCREENSHOT: SosSlider em ação]`

### Slide 12 — Boas práticas & limitações
- **Título**: Qualidade e limitações conhecidas
- **Coluna esquerda — Quality checklist cumprido**:
  - Min SDK 24 (~99% dos dispositivos)
  - Permissões pedidas em contexto, não no arranque
  - Estados de loading e erro em todos os ecrãs
  - Suporte completo a rotação (StateFlow)
  - Tema escuro Material 3 nativo
- **Coluna direita — Limitações honestas**:
  - Sem cache offline custom (depende do cache do Firestore)
  - Sem Health Connect (não centralizamos dados de saúde)
  - Sem testes instrumentados (`androidTest` por escrever)
  - Deteção de queda atualmente só por sensibilidade configurada

### Slide 13 — Conclusão & Future Work
- **Título**: Conclusões e próximos passos
- **Coluna esquerda — Lições aprendidas**:
  - `callbackFlow` foi a peça que destravou Firestore reativo
  - Foreground Service é mandatório para tracking sério
  - Compose acelera muito a iteração visual
  - Coroutines simplificam toda a stack assíncrona
- **Coluna direita — Future Work** (com destaque visual no primeiro ponto):
  - **Integração com wearables via Bluetooth Low Energy (BLE GATT)** — ligação a Polar H10 (Heart Rate Service `0x180D`) e protocolo custom VitalBand para deteção de quedas por hardware dedicado
  - Migrar para **Kotlin Multiplatform** (iOS)
  - Integrar **Health Connect** como fonte unificada de saúde
  - **Gemini Nano** para classificar tipo de atividade
  - **WorkManager** para sync diferido de atividades
- **Caixa final** (centrada): "Obrigado · Perguntas?"
- **Repositório**: [URL DO GIT]

---

## INDICAÇÕES FINAIS PARA O LLM

- Mantém todos os 13 slides com **estrutura visual consistente** (mesmo cabeçalho, rodapé, paleta).
- Quando vires `[INSERIR ...]`, deixa um **placeholder visualmente identificável** (caixa cinzenta com texto descritivo) — o utilizador vai substituir manualmente por screenshots reais.
- Não cortes nem fundas slides; preserva o número exato de 13.
- Não adiciones um slide final "Thank you" extra — a despedida está dentro do slide 13.
- Output final: ficheiro `.pptx` widescreen 16:9.
