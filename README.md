# Meu Plano — Android com MVI (Kotlin + Compose)

App de **autoatendimento de operadora**: consumo da franquia, contratação de pacotes extras, faturas com código de barras e **alertas automáticos de consumo em segundo plano**. Arquitetura **MVI** com fluxo unidirecional de dados.

> Segmento: **Telecomunicações**

## Stack
Kotlin 2.0 • Jetpack Compose (Material 3) • **MVI** (Intent → Reducer → State + Effect) • StateFlow • Channels • Coroutines (`async` paralelo) • **WorkManager** (`CoroutineWorker` periódico com constraints) • **DataStore Preferences** • **Koin** • Notificações (canal + permissão Android 13) • JUnit 4 • Turbine • kotlinx-coroutines-test (tempo virtual)

## MVI

```
  UI (Compose) ──HomeIntent──► Channel ──► handle() ──► efeitos colaterais (repositório, DataStore)
      ▲                                                       │
      │                                                  HomeResult
      │                                                       ▼
  StateFlow<HomeState> ◄──────────────── reduce(state, result)   ← função pura
      │
  Channel<HomeEffect> ──► Toast / copiar código de barras  (eventos de disparo único)
```

- **`reduce` é uma função pura**: toda mudança de estado passa por ela e é testada sem Android nem coroutines.
- **Efeitos de disparo único** (toast, clipboard) vão por `Channel`, não pelo estado, então não reaparecem ao girar a tela.
- **Intents em `Channel.UNLIMITED`**: nenhum intent é perdido antes de o coletor iniciar.
- **Proteção contra toque duplo** na contratação: o estado "comprando" é aplicado de forma síncrona antes da chamada de rede.

## Alerta de consumo em background
`UsageAlertWorker` roda a cada 6 h (somente com rede e bateria não baixa), consulta o consumo e aplica a `UsageAlertPolicy`:
- avisa **uma vez** ao cruzar o limite configurado (50%–95%, salvo no DataStore);
- avisa de novo quando a franquia **acaba**;
- usa retry com limite de tentativas em caso de falha.

## Como executar
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest   # reducer, política de alerta e ViewModel (paralelismo verificado com tempo virtual)
```
O `FakeTelecomRepository` simula o backend com latência, então o app roda sem servidor.
