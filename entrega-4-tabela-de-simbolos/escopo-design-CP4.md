<div align="center">

**UNIVERSIDADE FEDERAL DE UBERLÂNDIA – UFU**  
**FACULDADE DE COMPUTAÇÃO – FACOM**  
**BACHARELADO EM CIÊNCIA DA COMPUTAÇÃO**

# CONSTRUÇÃO DE COMPILADORES
## CHECKPOINT 4 — TABELA DE SÍMBOLOS E IDENTIFICAÇÃO
### Parte 1 — Desenho do ambiente e decisões de escopo

**Novembro de 2026**

</div>

---

## 1.1 Objetivo e convenções

O objetivo é projetar a identificação de nomes sobre a AST do CP3. Um **ambiente** é a pilha de escopos atualmente visíveis. O símbolo `x@L` representa a declaração de `x` na linha `L`. A busca começa no escopo mais interno e continua em direção ao global. As tabelas abaixo são **expectativas desenhadas antes da implementação da passada de identificação**, não resultados de execução.

**Convenções adotadas:** nível 0 = global; nível 1 = parâmetros e corpo externo da função; nível 2 = bloco `if`; nível 3 = bloco `while` interno. O corpo principal de uma função compartilha o escopo de seus parâmetros; blocos explícitos internos abrem novos escopos. Declarações globais de funções são registradas antes de visitar os respectivos corpos (pré-declaração de cabeçalhos).

## 2. Programa de referência com linhas numeradas

O exemplo abaixo é **sintaticamente válido na linguagem do CP3**, mas contém propositalmente um identificador fora de escopo. Assim, permite distinguir erro de sintaxe (CP2/CP3) de erro de identificação (CP4).

```text
 1  int x;
 2  int calcular(int p) {
 3      int y;
 4      x = p;
 5      if (true) {
 6          int x;
 7          x = p;
 8          while (false) {
 9              int z;
10              z = x;
11          }
12          x = z;
13      }
14      y = x;
15      return y;
16  }
```

Esse programa foi escolhido para **provocar um erro intencional** na linha 12: `z` já saiu do escopo. A declaração `x@6` sombreia `x@1` somente dentro do bloco do `if`. A função possui o parâmetro `p@2` e dois blocos aninhados (`if` e `while`). A análise sintática pode aceitar o programa; a identificação deve relatar o nome não declarado.

### 2.1 Desenho da pilha de ambientes

```text
GLOBAL — nível 0
┌──────────────────────────┐
│ x → x@1                  │
│ calcular → calcular@2    │
└──────────────────────────┘
             │ entra em calcular
             ▼
FUNÇÃO — nível 1
┌──────────────────────────┐
│ p → p@2                  │
│ y → y@3                  │
└──────────────────────────┘
             │ entra no if
             ▼
IF — nível 2
┌──────────────────────────┐
│ x → x@6  (sombreia x@1) │
└──────────────────────────┘
             │ entra no while
             ▼
WHILE — nível 3
┌──────────────────────────┐
│ z → z@9                  │
└──────────────────────────┘
```

**Visibilidade:** dentro do `while`, a ordem de procura é nível 3 → nível 2 → nível 1 → nível 0. Após a linha 11, o nível 3 é removido; `z@9` deixa de ser visível. Após a linha 13, o nível 2 é removido e `x` volta a resolver para `x@1`.

### 2.2 Ambiente após cada declaração e fechamento

| Ponto | Evento | Escopos ativos (do externo ao interno) | Nível atual |
|---|---|---|---:|
| Após L1 | Declara `x@1` | `{x@1}` | 0 |
| Após cabeçalho L2 | Declara `calcular@2` globalmente; abre função e declara `p@2` | `{x@1, calcular@2}` → `{p@2}` | 1 |
| Após L3 | Declara `y@3` | `{x@1, calcular@2}` → `{p@2, y@3}` | 1 |
| Após L5 | Abre bloco `if` | `{x@1, calcular@2}` → `{p@2, y@3}` → `{}` | 2 |
| Após L6 | Declara `x@6` | `{x@1, calcular@2}` → `{p@2, y@3}` → `{x@6}` | 2 |
| Após L8 | Abre bloco `while` | `{x@1, calcular@2}` → `{p@2, y@3}` → `{x@6}` → `{}` | 3 |
| Após L9 | Declara `z@9` | `{x@1, calcular@2}` → `{p@2, y@3}` → `{x@6}` → `{z@9}` | 3 |
| Após L11 | Fecha `while` | `{x@1, calcular@2}` → `{p@2, y@3}` → `{x@6}` | 2 |
| Após L13 | Fecha `if` | `{x@1, calcular@2}` → `{p@2, y@3}` | 1 |
| Após L16 | Fecha função | `{x@1, calcular@2}` | 0 |

### 2.3 Resolução de cada uso de identificador

| Linha | Uso | Busca / resolução |
|---|---|---|
| 4 | `x` (destino) | `x@1` — global |
| 4 | `p` (valor) | `p@2` — parâmetro |
| 7 | `x` (destino) | `x@6` — declaração interna, sombreia `x@1` |
| 7 | `p` (valor) | `p@2` — parâmetro |
| 10 | `z` (destino) | `z@9` — bloco `while` |
| 10 | `x` (valor) | `x@6` — bloco `if` |
| 12 | `x` (destino) | `x@6` — bloco `if` ainda aberto |
| 12 | `z` (valor) | **Não declarado no ambiente atual** — `z@9` saiu do escopo em L11 |
| 14 | `y` (destino) | `y@3` — função |
| 14 | `x` (valor) | `x@1` — global, pois `x@6` saiu do escopo em L13 |
| 15 | `y` (retorno) | `y@3` — função |

**Erro esperado desenhado manualmente:**

```text
linha 12: 'z' não declarado
```

> Os identificadores em posições de declaração (`x@1`, `calcular@2`, `p@2`, `y@3`, `x@6`, `z@9`) não são usos e não são resolvidos por `lookup`; são registrados por `declare`.

## 3. Estrutura planejada de Symbol

| Campo | Tipo proposto | Motivo e fase que o consulta |
|---|---|---|
| `name` | `String` | Chave para `declare`, `lookup` e mensagens de identificação (CP4). |
| `category` | `SymbolCategory` (`VARIABLE`, `PARAMETER`, `FUNCTION`, `PROCEDURE`) | Diferencia o papel do nome; identificação e verificações posteriores. |
| `declaration` | `Node` | Aponta para o nó da declaração na AST; identificação e análise semântica (CP4–CP5). |
| `level` | `int` | Registra o escopo de criação, útil para auditoria de sombreamento e análises posteriores (CP4). |
| `type` | campo futuro, inicialmente ausente ou `null` | Será preenchido/consultado na checagem de tipos (CP5); **não inferir agora**. |
| `offset` | campo futuro, inicialmente ausente | Deslocamento para organização de memória nas fases posteriores; **não calcular agora**. |

Cada `IdNode` de **uso** receberá futuramente `decl: Symbol?`, inicialmente `null`. Esse campo não é a declaração em si: é uma referência ao símbolo encontrado pela busca. A linguagem atual não possui declaração de constantes; essa categoria não será implementada no CP4.

## 4. As cinco decisões de projeto

### 4.1 Sombreamento e redeclaração

- **Escolha:** um nome em bloco interno pode ocultar o externo. Duas declarações do mesmo nome no **mesmo escopo** são erro; `declare` retorna `false` e preserva a primeira.
- **Alternativa descartada:** proibir qualquer repetição de nome em toda a cadeia de escopos.
- **Motivo:** o sombreamento respeita o escopo léxico e permite variáveis locais independentes; impedir redeclaração local evita ambiguidades.
- **Programa de teste:**

```text
int x;
int f() {
    int x;        // permitido: sombreia o global
    int x;        // erro: redeclaração no mesmo bloco
    return x;     // resolve para o primeiro x local
}
```

**Esperado:** uma mensagem de redeclaração na linha 4; `return x` resolve para `x@3`.

### 4.2 Declaração antes do uso

- **Escolha:** variáveis locais e globais precisam ser declaradas antes do uso na ordem textual; nomes de funções/procedimentos são pré-declarados no escopo que os contém antes da visita dos corpos.
- **Alternativa descartada:** permitir qualquer uso antecipado de variável com uma segunda passada de resolução.
- **Motivo:** mantém o comportamento de variáveis previsível e permite chamadas recursivas sem depender da ordem das funções.
- **Programa de teste:**

```text
int f() {
    x = 1;       // erro: x ainda não declarado
    int x;
    return x;    // resolve para x@3
}
```

**Esperado:** `linha 2: 'x' não declarado`.

### 4.3 Funções mutuamente recursivas

- **Escolha:** permitir funções e procedimentos mutuamente recursivos por **pré-declaração dos cabeçalhos** no escopo global, antes de visitar os corpos.
- **Alternativa descartada:** registrar a função somente quando sua implementação completa for visitada.
- **Motivo:** chamadas entre `a` e `b` não devem depender da ordem textual das definições.
- **Programa de teste:**

```text
int a() { return b(); }
int b() { return a(); }
```

**Esperado:** `b` na linha 1 resolve para `b@2`, e `a` na linha 2 resolve para `a@1`; nenhum erro de identificação. O exemplo verifica somente nomes, não término da recursão. **Limite do CP3:** o parser aceita funções globais, mas não funções aninhadas; estas não serão acrescentadas neste checkpoint.

### 4.4 Parâmetros e corpo da função

- **Escolha:** parâmetros e declarações locais do corpo **externo** da função compartilham o mesmo escopo. Blocos internos `{...}` abrem escopos próprios.
- **Alternativa descartada:** abrir um escopo adicional para o corpo imediatamente após declarar os parâmetros.
- **Motivo:** evita que uma variável do corpo possa sombrear acidentalmente um parâmetro na própria função; a visita ao `BlockNode` raiz do corpo deve reconhecer esse caso especial sem abrir outro escopo.
- **Programa de teste:**

```text
void f(int a) {
    int a;        // erro: redeclara o parâmetro no mesmo escopo
}
```

**Esperado:** `linha 2: 'a' já declarado neste escopo (linha 1)`.

### 4.5 Espaços de nomes

- **Escolha:** variáveis, parâmetros, funções e procedimentos compartilham **um espaço de nomes por escopo**, como em C para identificadores ordinários.
- **Alternativa descartada:** manter mapas separados para funções e variáveis, permitindo ambos com o mesmo nome no mesmo bloco.
- **Motivo:** simplifica `lookup` e evita ambiguidades entre uso de valor e chamada de função. A categoria do símbolo permite verificações de uso apropriado em etapas posteriores.
- **Programa de teste:**

```text
int f() { return 1; }
int f;             // erro: nome f já declarado no escopo global
```

**Esperado:** `linha 2: 'f' já declarado neste escopo (linha 1)`; a função `f@1` permanece registrada.
