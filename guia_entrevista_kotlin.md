# Guia de Entrevista — Kotlin (VitalRoute)

Perguntas teóricas mais comuns sobre Kotlin que podem aparecer numa entrevista. Foco no que é essencial e no que é relevante para desenvolvimento Android (o caso do teu projeto).

---

## 1. Fundamentos do Kotlin

### O que é o Kotlin?
Linguagem de programação moderna, estaticamente tipada, desenvolvida pela JetBrains. Corre na JVM (Java Virtual Machine), é 100% interoperável com Java e é a linguagem oficial recomendada pela Google para desenvolvimento Android desde 2017.

### Quais as vantagens do Kotlin sobre Java?
- **Null safety** integrada no sistema de tipos (evita NullPointerExceptions).
- **Sintaxe mais concisa** (menos código boilerplate).
- **Data classes**, extension functions, smart casts.
- **Coroutines** para programação assíncrona.
- **Interoperabilidade total** com Java.
- **Funcional + Orientada a Objetos** (suporta os dois paradigmas).

### O Kotlin é compilado ou interpretado?
Compilado. Pode ser compilado para bytecode JVM, JavaScript (Kotlin/JS) ou código nativo (Kotlin/Native).

---

## 2. Variáveis e Tipos

### Diferença entre `val` e `var`?
- `val` → variável imutável (read-only, equivalente a `final` em Java). Não pode ser reatribuída.
- `var` → variável mutável, pode ser reatribuída.

```kotlin
val name = "Duarte"   // não pode mudar
var age = 21          // pode mudar
```

### O que é inferência de tipos?
O compilador deduz automaticamente o tipo da variável a partir do valor atribuído. `val x = 10` → o compilador sabe que `x` é `Int`.

### Tipos básicos importantes
`Int`, `Long`, `Double`, `Float`, `Boolean`, `Char`, `String`. Em Kotlin **tudo é objeto** (não há tipos primitivos como em Java, embora o compilador otimize para primitivos quando possível).

---

## 3. Null Safety (MUITO IMPORTANTE)

### Como funciona null safety em Kotlin?
Por defeito, **nenhuma variável pode ser null**. Para permitir nulls, declaras o tipo com `?`:

```kotlin
var name: String = "Duarte"     // não pode ser null
var nick: String? = null        // pode ser null
```

### Operadores importantes:
- **`?.`** (safe call) → chama o método só se não for null: `nick?.length`
- **`?:`** (Elvis operator) → valor por defeito se for null: `val len = nick?.length ?: 0`
- **`!!`** (non-null assertion) → força não-null (lança NPE se for null). **Evitar** em código real.
- **`as?`** → cast seguro, devolve null se falhar.

---

## 4. Funções

### Como se declara uma função?
```kotlin
fun soma(a: Int, b: Int): Int {
    return a + b
}
// Versão expression-body:
fun soma(a: Int, b: Int): Int = a + b
```

### Argumentos por defeito e nomeados
```kotlin
fun saudacao(nome: String, msg: String = "Olá") = "$msg, $nome"
saudacao(nome = "Duarte")  // usa o default
```

### O que são higher-order functions?
Funções que recebem outras funções como parâmetro ou devolvem funções. Ex:
```kotlin
list.filter { it > 5 }.map { it * 2 }
```

### O que são lambdas?
Funções anónimas. Sintaxe: `{ parâmetros -> corpo }`. Se houver um único parâmetro, usa-se `it`.

### Extension functions
Permitem **adicionar métodos a classes existentes** sem as alterar:
```kotlin
fun String.firstChar(): Char = this[0]
"Duarte".firstChar()  // 'D'
```

---

## 5. Classes e OOP

### Classes em Kotlin
```kotlin
class Pessoa(val nome: String, var idade: Int)
```
O construtor primário aparece no header da classe. `val`/`var` no construtor cria automaticamente a propriedade.

### Data classes
Classes desenhadas para guardar dados. O compilador gera automaticamente:
- `equals()` / `hashCode()`
- `toString()`
- `copy()`
- `componentN()` (para destructuring)

```kotlin
data class User(val name: String, val age: Int)
```

**Muito usadas em Android para modelos** (ex: representar uma localização GPS, um utilizador, etc.).

### Sealed classes
Hierarquia de classes "fechada" — todas as subclasses têm de estar no mesmo ficheiro. Útil para representar estados finitos (ex: `Loading`, `Success`, `Error` numa UI).

```kotlin
sealed class Resultado {
    data class Sucesso(val data: String) : Resultado()
    data class Erro(val msg: String) : Resultado()
    object Loading : Resultado()
}
```
O compilador sabe quais os tipos possíveis → útil em `when` (não precisa de `else`).

### `object` keyword (singleton)
Cria um singleton (única instância):
```kotlin
object DatabaseManager { fun connect() { ... } }
```

### `companion object`
Equivalente a métodos/campos `static` em Java. Pertence à classe e não a instâncias:
```kotlin
class User { companion object { const val MAX_AGE = 120 } }
User.MAX_AGE
```

### Visibilidade
`public` (default), `private`, `protected`, `internal` (visível dentro do mesmo módulo).

### Herança
Por defeito todas as classes são `final`. Para serem herdadas têm de ser marcadas `open`:
```kotlin
open class Animal
class Cao : Animal()
```

---

## 6. Scope Functions (perguntas frequentes!)

`let`, `run`, `with`, `apply`, `also` — todas executam um bloco com um objeto, mas diferem no objeto de referência e no valor de retorno.

| Função | Referência | Devolve |
|--------|-----------|---------|
| `let`  | `it`      | resultado do lambda |
| `run`  | `this`    | resultado do lambda |
| `with` | `this`    | resultado do lambda |
| `apply`| `this`    | o próprio objeto |
| `also` | `it`      | o próprio objeto |

Exemplos típicos:
- `apply` → configurar um objeto após criar: `Button().apply { text = "OK" }`
- `let` → executar algo se não for null: `nick?.let { print(it) }`
- `also` → side effects (logs, etc.) mantendo o objeto.

---

## 7. Smart Casts e `is`

O Kotlin faz **smart cast** automaticamente:
```kotlin
if (obj is String) {
    println(obj.length)  // já tratado como String
}
```

---

## 8. `when` expression

Substitui `switch` mas é muito mais poderoso (pode usar ranges, tipos, condições):
```kotlin
when (x) {
    in 1..10 -> "pequeno"
    is String -> "texto"
    else -> "outro"
}
```

---

## 9. Collections

- `List<T>` (imutável) vs `MutableList<T>` (mutável).
- Mesma distinção para `Set`, `Map`.
- Operações funcionais: `map`, `filter`, `reduce`, `fold`, `forEach`, `groupBy`, `sortedBy`, etc.

---

## 10. `lateinit` vs `lazy`

- **`lateinit var`** → variável inicializada mais tarde, mas antes do primeiro uso. Só para `var` não-nulos de tipos não primitivos. Comum em Android (ex: views inicializadas em `onCreate`).
- **`by lazy { ... }`** → só para `val`. O valor é calculado na primeira chamada e cacheado.

```kotlin
val data: String by lazy { computeExpensiveData() }
lateinit var name: String
```

---

## 11. Coroutines (MUITO IMPORTANTE para Android)

### O que são coroutines?
Mecanismo do Kotlin para **programação assíncrona**, mais leve que threads. Permitem escrever código assíncrono de forma sequencial.

### Conceitos-chave:
- **`suspend fun`** → função que pode ser suspensa sem bloquear a thread. Só pode ser chamada dentro de outra coroutine ou suspend function.
- **`CoroutineScope`** → define o ciclo de vida das coroutines (ex: `viewModelScope`, `lifecycleScope` no Android).
- **`launch`** → inicia coroutine sem retorno (fire-and-forget).
- **`async/await`** → inicia coroutine e devolve um `Deferred<T>` (com resultado).
- **`Dispatchers`** → indicam onde corre a coroutine:
  - `Dispatchers.Main` → UI thread
  - `Dispatchers.IO` → operações de I/O (rede, ficheiros)
  - `Dispatchers.Default` → CPU-intensive

### No teu projeto (VitalRoute):
Coroutines são úteis para:
- Recolher dados de GPS continuamente sem bloquear a UI.
- Fazer pedidos HTTP (partilha de localização).
- Ler/escrever ficheiros (cache de tiles offline).

### Flow
API reativa baseada em coroutines. Útil para **streams contínuos de dados** (ex: sensores, GPS, atualizações de localização em tempo real). Substitui `LiveData` em muitos casos.

```kotlin
fun locationFlow(): Flow<Location> = flow { ... }
```

---

## 12. Outras perguntas que podem aparecer

### O que é `inline`?
Funções `inline` são "inseridas" no local da chamada pelo compilador (evita o custo de criar objetos para lambdas). Usado em `let`, `apply`, etc.

### O que são generics e variância (`in`/`out`)?
- `out T` → covariância (produtor, só lê)
- `in T` → contravariância (consumidor, só escreve)

### Diferença entre `==` e `===`?
- `==` → equivalente a `.equals()` (igualdade estrutural).
- `===` → identidade de referência (mesma instância em memória).

### O que é destructuring?
Permite extrair vários valores de um objeto:
```kotlin
val (nome, idade) = pessoa
```

### `typealias`
Cria um alias para um tipo:
```kotlin
typealias UserMap = Map<String, User>
```

---

## 13. Perguntas Android (que se cruzam com o projeto)

- **Activity vs Fragment**: Activity é um ecrã; Fragment é uma sub-secção reutilizável dentro de uma Activity.
- **Lifecycle**: `onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`.
- **Jetpack Compose** (se usares): UI declarativa baseada em funções `@Composable`.
- **ViewModel**: guarda estado de UI que sobrevive a rotações.
- **Permissions runtime**: para localização (`ACCESS_FINE_LOCATION`), sensores, etc., tens de pedir em runtime no Android 6+.
- **Foreground Service**: necessário para gravar GPS com a app em background (caso do teu tracker).
- **SensorManager**: API Android para acelerómetro/giroscópio (deteção de queda).
- **Room**: ORM oficial para SQLite em Android (útil para guardar histórico de rotas).
- **Retrofit / Ktor**: clientes HTTP para chamadas API (ex: partilha de localização).

---

## Dica final

Se te perguntarem algo que não sabes, **não inventes**. Diz que ainda não exploraste essa parte em profundidade, mas explica o que sabes do conceito relacionado. Mostra capacidade de raciocínio, não memorização.

Boa sorte! 🚀
