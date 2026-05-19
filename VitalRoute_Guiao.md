# Guião da apresentação VitalRoute — 15 minutos

> **Equipa**: A = Duarte Lourenço · B = João Silva
> **Total**: 15:00 minutos · 12 slides · média ≈ 1:15 por slide
> **Notação**: `[D]` = Duarte fala · `[J]` = João fala · `(...)` = ação no slide ou nota de palco

| # | Slide | Quem | Tempo |
|---|---|---|---|
| 1 | Capa | D | 0:30 |
| 2 | Problema & Conceito | D | 1:15 |
| 3 | Journey Map | D | 1:30 |
| 4 | Tour da app | D → handover | 1:15 |
| 5 | Arquitetura geral | J | 1:15 |
| 6 | UI Declarativa | J | 1:15 |
| 7 | Firestore reativo | J | 1:30 |
| 8 | Navegação + APIs | J → handover | 1:00 |
| 9 | Localização e Mapas | D | 1:30 |
| 10 | Gravação em background | D | 1:15 |
| 11 | Sistema SOS | D → handover | 1:30 |
| 12 | Conclusão & Future Work | J | 1:15 |

---

## Slide 1 — Capa · `[D]` · 0:30

> [D] "Bom dia. Sou o Duarte Lourenço e este é o João Silva. Vimos hoje apresentar o nosso projeto da cadeira de Introdução à Computação Móvel — chama-se **VitalRoute**.
>
> Numa frase: é uma aplicação Android para atletas ao ar livre que junta, no mesmo fluxo, tracking de fitness e um sistema automático de emergência. Vamos passar pelo conceito, depois pela arquitetura, e fechamos com as decisões técnicas mais interessantes."

*(Avançar para slide 2.)*

---

## Slide 2 — Problema & Conceito · `[D]` · 1:15

> [D] "O problema é simples de descrever mas pouco resolvido no mercado.
>
> Ciclistas e corredores treinam tipicamente sozinhos — antes ou depois do trabalho, ao fim-de-semana, em estradas com pouco trânsito. Quando algo corre mal, **acidentes sem testemunhas são críticos**: uma queda de bicicleta a 30 km/h, uma indisposição súbita, e o atleta pode ficar minutos ou horas sem ajuda.
>
> Olhámos para o mercado e vimos um vazio claro:
>
> - O **Strava** e a **Garmin Connect** sabem tudo sobre treino, mas pouco ou nada sobre segurança.
> - Apps de **botão de pânico** sabem tudo sobre segurança, mas não percebem nada do contexto desportivo — não sabem distinguir uma queda real de pôr o telemóvel na mochila.
>
> A nossa proposta é juntar as duas coisas num só fluxo: o VitalRoute é, ao mesmo tempo, um fitness tracker e um sistema automático de emergência."

*(Pausa breve. Avançar.)*

---

## Slide 3 — Journey Map · `[D]` · 1:30

> [D] "Para mostrar como isto funciona na prática, fizemos o journey map de uma persona típica — a Marta, ciclista de fim-de-semana, sai sozinha duas vezes por semana e quer segurança sem interromper o treino.
>
> O percurso dela na app é o seguinte *(acompanhar com os passos no slide)*:
>
> 1. Abre a app e **consulta a meteorologia** e a camada de mapa de ciclismo — decide se o tempo está bom.
> 2. Antes de sair, vai à aba Segurança e **confirma os contactos de confiança** que vão receber alertas.
> 3. Carrega em 'Iniciar Gravação' — o **cronómetro e métricas em tempo real** começam.
> 4. A meio do percurso cai. A sensibilidade configurada na app **deteta um impacto provável**.
> 5. Aparece-lhe no ecrã o **SOS Slider** — dá-lhe 10 segundos para cancelar se foi falso positivo.
> 6. Se ela não cancelar, é **enviado SMS automático** para os contactos com a última localização GPS conhecida.
>
> O touchpoint principal é a app, mas há touchpoints secundários importantes: o SMS, os contactos do telemóvel e os mapas do OpenStreetMap. Vamos voltar a estes três pontos na segunda metade da apresentação."

*(Avançar.)*

---

## Slide 4 — Tour da app · `[D]` · 1:15 (handover no fim)

> [D] "Para fechar a parte de conceito, um tour rápido pelas cinco áreas da app, organizadas numa bottom navigation bar consistente.
>
> - **Início** — dashboard semanal com estatísticas reais (quilómetros, tempo ativo, número de incidentes) e estado dos sensores.
> - **Mapas** — OpenStreetMap com camada CyclOSM para ciclovias e meteorologia em tempo real no canto.
> - **Iniciar** — cronómetro, grelha 2×2 com distância, velocidade, elevação e calorias, e o slider SOS sempre acessível.
> - **Segurança** — contactos de confiança, sensibilidade de queda configurável e zonas seguras.
> - **Diário** — histórico de todas as atividades, recordes pessoais calculados automaticamente e resumo do mês.
>
> Tudo isto está sustentado por uma arquitetura que o João vai explicar agora."

*(Passar o telecomando ao João. Recuar um passo.)*

---

## Slide 5 — Arquitetura geral · `[J]` · 1:15

> [J] "Obrigado, Duarte. Vamos então para a parte técnica.
>
> A arquitetura segue o padrão **MVVM com fluxo unidirecional de dados** — exatamente o que está recomendado na Android Architecture Guide.
>
> Separámos a app em três camadas *(acompanhar setas no slide)*:
>
> - **Camada 1 — UI Layer**: composta exclusivamente por *Compose Screens* — Home, Maps, Recording, Security, Diary e Auth.
> - **Camada 2 — State Holders**: cada ecrã tem o seu ViewModel, que expõe um `StateFlow<UiState>`. A UI consome esse fluxo com `collectAsStateWithLifecycle`, o que garante que o estado é cancelado corretamente quando o ecrã sai de cena.
> - **Camada 3 — Data Layer**: os ViewModels nunca falam diretamente com Firebase, com APIs nem com nada externo — falam com **repositórios**. Temos o FirestoreRepository, o WeatherRepository, o NominatimRepository e o SosManager.
>
> A regra de ouro é a do slide: **state flows down, events flow up**. A UI renderiza estado imutável e devolve eventos; toda a lógica fica fora dos composables. Isto torna o comportamento previsível e os ecrãs trivialmente substituíveis."

*(Avançar.)*

---

## Slide 6 — UI Declarativa · `[J]` · 1:15

> [J] "Zoom-in agora na camada de UI.
>
> A app é construída inteiramente em **Jetpack Compose com Material 3**. Os composables são *stateless* — não guardam estado interno — e usamos *state hoisting* para passar tanto o estado como os callbacks de eventos.
>
> O snippet em baixo *(apontar)* é representativo. O `HomeScreen` recebe um `HomeViewModel` por injeção, faz `collectAsStateWithLifecycle` do `uiState`, e passa esse estado mais um callback `onSosClick` para um composable filho stateless. É só isto — toda a regra de negócio fica no ViewModel.
>
> O tema escuro está definido em `Theme.kt` e `Color.kt` — escolhemos dark theme propositadamente porque a app é usada muitas vezes em condições de baixa luminosidade, ao início da manhã ou ao fim do dia.
>
> Uma consequência prática desta arquitetura é que a app **sobrevive a mudanças de configuração** — rotação do ecrã, troca de tema, mudança de língua — sem perder estado. O `StateFlow` no ViewModel garante isso."

*(Avançar.)*

---

## Slide 7 — Firestore reativo · `[J]` · 1:30

> [J] "Agora a peça central da camada de dados: o Firestore.
>
> Escolhemos Firestore por três motivos: persistência na cloud, sincronização entre dispositivos, e — talvez o mais importante — **listeners em tempo real**.
>
> O modelo de dados é por utilizador *(acompanhar esquema)*: cada utilizador tem um documento raiz com nome e email, e três sub-coleções — `activities` para o histórico, `contacts` para os contactos de confiança, e `settings` para preferências como a sensibilidade de queda.
>
> A ponte entre o Firestore e Kotlin é feita com **`callbackFlow`**. Os listeners do Firestore são por natureza imperativos, em callbacks; o `callbackFlow` converte-os em `Flow<T>` Kotlin idiomáticos. Isto significa que quando o utilizador adiciona um contacto noutro dispositivo, ou quando uma atividade é guardada, a UI atualiza-se automaticamente — sem qualquer pedido manual.
>
> Em termos de segurança, as regras do Firestore garantem que cada utilizador só lê e escreve os seus próprios documentos, indexados pelo UID. A autenticação é feita por email e password, com Firebase Auth, e há um `AuthStateListener` ativo para reagir a logouts e sessões expiradas.
>
> O fluxo completo é: Firestore Listener → `callbackFlow` → `StateFlow<UiState>` → Compose UI."

*(Avançar.)*

---

## Slide 8 — Navegação + APIs · `[J]` · 1:00 (handover no fim)

> [J] "Para fechar a parte de software architecture, dois pontos mais rápidos.
>
> **Navegação**: usamos `Navigation Compose`. Há cinco destinos principais — Home, Maps, Recording, Security, Diary — mais ecrãs detalhados como Settings, ActivityDetail e Auth. A bottom bar é automaticamente escondida nos ecrãs detalhados para o utilizador focar na tarefa.
>
> **APIs externas**: usamos Retrofit com Gson para três serviços REST, todos públicos e sem API key:
>
> - **Open-Meteo** para temperatura, humidade e vento.
> - **Nominatim** para geocoding reverso baseado no OpenStreetMap.
> - **Overpass** para pontos de interesse do OSM.
>
> Todas as chamadas são `suspend functions` lançadas no `viewModelScope`, o que significa zero bloqueios no main thread.
>
> Agora o Duarte vai entrar na parte mais sensorial e contextual da app."

*(Passar o telecomando ao Duarte.)*

---

## Slide 9 — Localização e Mapas · `[D]` · 1:30

> [D] "Obrigado, João.
>
> Esta foi uma das decisões técnicas que mais discutimos: como obter localização e como mostrar mapas.
>
> **Para localização**, usamos o `FusedLocationProviderClient` dos Play Services, que é a recomendação oficial — em vez do antigo `LocationManager` — porque combina inteligentemente GPS, Wi-Fi e torres de celular e gere o consumo de bateria automaticamente.
>
> Em termos de **permissões**, pedimos `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION`, mas, importante, **pedimo-las em contexto** — quando o utilizador carrega no botão de gravação, não quando abre a app pela primeira vez. Isto é uma boa prática da própria documentação da Google e melhora muito a taxa de aceitação. Se a permissão não for concedida, o mapa abre centrado em Aveiro como fallback.
>
> **Para mapas**, escolhemos **OSMDroid em vez do Google Maps SDK**. Os motivos são três:
>
> 1. **Sem API key e sem billing** — o Google Maps obriga a abrir uma conta de faturação, mesmo no plano gratuito. Para um projeto académico isto é fricção desnecessária.
> 2. **Open-source** — controlamos o stack inteiro.
> 3. **Camadas especializadas** — temos o **CyclOSM**, uma camada feita por e para ciclistas, que mostra ciclovias, trilhos e infra-estrutura ciclável que o Google Maps simplesmente não tem.
>
> O ponto azul de localização no mapa é desenhado com `MyLocationOverlay`."

*(Avançar.)*

---

## Slide 10 — Gravação em background · `[D]` · 1:15

> [D] "Quando a Marta inicia uma gravação, surge um problema: ela vai bloquear o telemóvel, pô-lo no bolso ou na bolsa de bicicleta, e o Android é agressivo a matar processos em background para poupar bateria.
>
> Aplicámos a *decision path* das aulas *(apontar para o slide)*. A pergunta é: o tracking sobrevive a app estar em background? **Sim**. É uma tarefa visível ao utilizador? **Sim** — o utilizador sabe que está a gravar. Resposta: **Foreground Service**.
>
> Implementámos o `RecordingService` com `foregroundServiceType=\"location\"` e a permissão `FOREGROUND_SERVICE_LOCATION` declarada no manifesto — esta permissão tornou-se obrigatória no Android moderno.
>
> Há três consequências práticas:
>
> 1. Aparece uma **notificação persistente** durante toda a gravação, com a duração e a distância — não é cosmético, é exigência do sistema.
> 2. O serviço **sobrevive ao ecrã apagado**: não perdemos GPS quando o utilizador bloqueia o telemóvel.
> 3. Se o sistema precisar mesmo de memória, o foreground service é o último a morrer."

*(Avançar.)*

---

## Slide 11 — Sistema SOS · `[D]` · 1:30 (handover no fim)

> [D] "Cheguei à feature que dá nome ao projeto: o sistema SOS.
>
> O fluxo, da deteção ao envio, está dividido em quatro passos no slide:
>
> 1. **Queda detetada** — a sensibilidade configurada pelo utilizador identifica um impacto provável durante o percurso.
> 2. **`SosSlider`** — aparece no ecrã um slider grande e vermelho, com **10 segundos** para cancelar. Este buffer é fundamental: queremos que o sistema falhe em segurança, não que ligue para os bombeiros porque o telemóvel caiu na mesa.
> 3. **`SosManager` envia SMS** — usa o `SmsManager` nativo do Android para disparar mensagens para todos os contactos marcados com `sosEnabled` no Firestore.
> 4. **Localização GPS na mensagem** — a SMS inclui a última posição conhecida no formato 'Preciso de ajuda. Última localização: lat, lon.'
>
> Há três pormenores técnicos que vale a pena destacar — todos envolvem Intents, que vimos nas aulas:
>
> - O **picker de contactos** é feito com Intent implícito — `ACTION_PICK` em `Contacts.CONTENT_URI`. Reutilizamos o picker nativo do Android em vez de implementar o nosso.
> - A **exportação de atividades** em formato GPX ou CSV é partilhada via `FileProvider` mais um Intent `ACTION_SEND`, o que dá ao utilizador a possibilidade de mandar para o Strava, WhatsApp, email — qualquer app.
> - As permissões `SEND_SMS` e `READ_CONTACTS` são pedidas só quando são necessárias — `READ_CONTACTS` quando o utilizador carrega em 'Adicionar Contacto', `SEND_SMS` na primeira tentativa de SOS.
>
> Com isto fecho a parte técnica. João, terás o fecho."

*(Passar o telecomando ao João.)*

---

## Slide 12 — Conclusão & Future Work · `[J]` · 1:15

> [J] "Para fechar, organizei o slide em três colunas: o que cumprimos, as nossas limitações, e o que faríamos a seguir.
>
> **Quality checklist cumprido** *(ler de forma rápida)*: Min SDK 24, o que cobre cerca de 99% dos dispositivos Android no mercado. Permissões pedidas sempre em contexto. Estados de loading e erro tratados em todos os ecrãs. Rotação suportada via `StateFlow`. Tema escuro Material 3 nativo.
>
> **Limitações honestas** — e esta coluna importa-nos:
>
> - Não temos cache offline custom; dependemos do cache que o Firestore traz por defeito.
> - Não integrámos Health Connect, o que seria útil para juntar dados de outras apps de fitness.
> - Não temos testes instrumentados — só temos unitários básicos.
> - E talvez o mais importante: a deteção de queda é **apenas por sensibilidade configurada**. A validação real do impacto é limitada.
>
> **Future Work** *(com ênfase no primeiro ponto)*:
>
> - **Integração com wearables via Bluetooth Low Energy** — ligar a um Polar H10 pelo Heart Rate Service `0x180D` ou a um dispositivo VitalBand custom para deteção de quedas por hardware dedicado. Isto resolve diretamente a limitação que acabámos de admitir.
> - Migração para **Kotlin Multiplatform** para uma versão iOS.
> - Integração com **Health Connect** como fonte unificada de dados de saúde.
> - **Gemini Nano** para classificar automaticamente o tipo de atividade.
> - **WorkManager** para sync diferido de atividades quando a rede falha.
>
> O repositório está disponível no link do slide. Obrigado pela atenção — estamos prontos para perguntas."

*(Manter slide visível. Dar um passo atrás, lado a lado com o Duarte.)*

---

## Anexo — Cábula para perguntas frequentes

**P: Porque não usaram Room?**
> Os nossos dados não precisam de existir só localmente — todos os dados do utilizador (atividades, contactos, definições) são por natureza para sincronizar entre dispositivos. O Firestore traz um cache offline que cobre o caso de uso, sem precisarmos de implementar uma camada de sincronização nós próprios. Se tivéssemos uma feature totalmente offline-first, aí Room faria sentido.

**P: Porque OSMDroid e não Google Maps?**
> Três motivos: sem API key e sem billing, é open-source, e tem a camada CyclOSM especializada em ciclismo. Para uma app focada em ciclistas, CyclOSM é melhor do que o tile padrão do Google.

**P: Como detetam a queda exatamente?**
> Honestamente, só temos hoje uma sensibilidade configurada pelo utilizador — Baixa, Média ou Alta. Não temos ainda integração com acelerómetro do telemóvel nem com wearables. É a próxima feature do roadmap, com BLE.

**P: O SMS é mesmo enviado ou é só uma simulação?**
> É mesmo enviado, via `SmsManager.sendTextMessage`. Por isso a permissão `SEND_SMS` é mandatória e está justificada em contexto.

**P: Têm autenticação por Google?**
> Não, só email e password com Firebase Auth. Poderia ser estendido facilmente — o Firebase Auth suporta vários providers — mas para a defesa académica achámos que o flow de email/password era suficiente.

**P: Como gerem permissões em runtime?**
> Pedimos no momento em que são necessárias, usando o launcher do Activity Result API. Localização no início da gravação, contactos quando o utilizador adiciona um contacto, SMS na primeira tentativa de SOS. Nunca pedimos no arranque.

**P: O Foreground Service consome muita bateria?**
> O `FusedLocationProviderClient` é otimizado para gerir consumo — combina GPS com Wi-Fi e cell tower data. Configurámos updates a cada 5 segundos durante gravação, que é um compromisso razoável entre precisão e bateria.

---

## Notas para ensaio

- **Tempo total**: 15:00. Já considerei pausas naturais. Se o ensaio passar dos 15:30, cortar primeiro do slide 4 (tour) e do slide 12 (conclusão).
- **Handovers**: três no total — slide 4→5 (D→J), slide 8→9 (J→D), slide 11→12 (D→J). Treinar especificamente estas transições.
- **Pontos críticos para não falhar**: slide 7 (Firestore + callbackFlow), slide 10 (decision path do Foreground Service), slide 11 (fluxo SOS). São onde o júri vai prestar mais atenção técnica.
- **Demo opcional**: se houver tempo no fim, mostrar a app a fazer login e gravar uns segundos no telemóvel. Tempo extra ≈ 1 min.
