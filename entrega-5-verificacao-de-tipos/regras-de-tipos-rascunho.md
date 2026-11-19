# Regras de tipos — RASCUNHO para a Parte 1

> **Atenção:** a Parte 1 do CP5 deve ser **escrita à mão** pelo grupo e entregue como `tipos-a-mao.pdf` (foto ou digitalização). Este arquivo é só o rascunho que orientou o código. Copiem no papel o que concordarem, mudem o que discordarem (e avisem para ajustar o código e os testes) e **não entreguem este `.md` no lugar do PDF**.

Notação: `Γ ⊢ e : T` = "no ambiente Γ (a tabela de símbolos decorada pela identificação), a expressão `e` tem tipo `T`". Premissas acima da linha, conclusão abaixo, condição ao lado.

Tipos: `int`, `double`, `bool`, `char`, `string`; `void` (só retorno de procedimento); `erro` (tipo interno: expressão que já gerou erro).

---

## 1. Regras (uma por forma de nó)

### Literais

```
[LIT-INT]  ─────────────────   (n sem ponto)        [LIT-DOUBLE] ─────────────────   (n com ponto)
            Γ ⊢ n : int                                          Γ ⊢ n : double

[LIT-BOOL] ─────────────────────────────            [LIT-CHAR]   ──────────────
            Γ ⊢ true : bool    Γ ⊢ false : bool                  Γ ⊢ 'c' : char

[LIT-STRING] ────────────────
              Γ ⊢ "s" : string
```

### Identificador

```
             Γ(x) = variável ou parâmetro de tipo T              Γ(f) = função ou procedimento
[ID]         ──────────────────────────────────────  [ID-SUBPROG] ─────────────────────────────  (erro, decisão 11)
             Γ ⊢ x : T                                            Γ ⊢ f : erro
```

`x` sem declaração: `Γ ⊢ x : erro`, **sem nova mensagem** (a identificação já reportou).

### Compatibilidade (usada por atribuição, inicialização, argumento e return)

```
            T = V                         V = int ou V = char               V = char
[COMPAT]  ─────────      [ALARGA-DOUBLE] ───────────────────   [ALARGA-INT] ──────────
            V ≼ T                          V ≼ double                        V ≼ int

[COMPAT-ERRO]  T = erro ou V = erro
               ────────────────────
                     V ≼ T
```

`V ≼ T` = "um valor do tipo V pode ser guardado onde se espera T" (alargamento `char → int → double`).

### Aritméticos

```
              Γ ⊢ e1 : T1   Γ ⊢ e2 : T2   T1, T2 ∈ {int, char}
[ARIT-INT]    ─────────────────────────────────────────────────   op ∈ { + - * / }
              Γ ⊢ e1 op e2 : int

              Γ ⊢ e1 : T1   Γ ⊢ e2 : T2   T1, T2 ∈ {int, char, double}   T1 = double ou T2 = double
[ARIT-DOUBLE] ──────────────────────────────────────────────────────────────────────────────────────   op ∈ { + - * / }
              Γ ⊢ e1 op e2 : double

              Γ ⊢ e1 : string   Γ ⊢ e2 : string
[CONCAT]      ──────────────────────────────────
              Γ ⊢ e1 + e2 : string
```

### Relacionais, igualdade e lógicos

```
              Γ ⊢ e1 : T1   Γ ⊢ e2 : T2   T1, T2 ∈ {int, char, double}
[REL]         ──────────────────────────────────────────────────────   op ∈ { < <= > >= }
              Γ ⊢ e1 op e2 : bool

              Γ ⊢ e1 : T1   Γ ⊢ e2 : T2   T1, T2 ∈ {int, char, double}
[IGUAL-NUM]   ──────────────────────────────────────────────────────   op ∈ { == != }
              Γ ⊢ e1 op e2 : bool

              Γ ⊢ e1 : T   Γ ⊢ e2 : T   T ∈ {bool, string}
[IGUAL-MESMO] ────────────────────────────────────────────   op ∈ { == != }
              Γ ⊢ e1 op e2 : bool

              Γ ⊢ e1 : bool   Γ ⊢ e2 : bool
[LOG]         ──────────────────────────────   op ∈ { && || }
              Γ ⊢ e1 op e2 : bool
```

### Unários

```
             Γ ⊢ e : T   T ∈ {int, char}           Γ ⊢ e : double             Γ ⊢ e : bool
[NEG-INT]    ───────────────────────  [NEG-DOUBLE] ─────────────────   [NAO]  ──────────────
             Γ ⊢ -e : int                          Γ ⊢ -e : double            Γ ⊢ !e : bool
```

### Propagação do erro (sem cascata)

```
              Γ ⊢ e1 : erro  ou  Γ ⊢ e2 : erro              Γ ⊢ e : erro
[ERRO-BIN]    ──────────────────────────────    [ERRO-UN]  ──────────────
              Γ ⊢ e1 op e2 : erro  (sem msg)               Γ ⊢ op e : erro  (sem msg)
```

### Comandos

```
              Γ(x) = variável/parâmetro de tipo T    Γ ⊢ e : V    V ≼ T
[ATRIB]       ──────────────────────────────────────────────────────────
              Γ ⊢ x = e ; ok

              Γ ⊢ e : V    V ≼ T          (e é verificado ANTES de x existir)
[DECL]        ──────────────────
              Γ ⊢ T x = e ; ok

              Γ ⊢ e : bool   Γ ⊢ S1 ok   Γ ⊢ S2 ok               Γ ⊢ e : bool   Γ ⊢ S ok
[IF]          ─────────────────────────────────────   [WHILE]    ───────────────────────
              Γ ⊢ if (e) S1 else S2 ok                          Γ ⊢ while (e) S ok

              Γ ⊢ S1 ok ... Γ ⊢ Sn ok                            Γ ⊢ e : T,  T ≠ void
[BLOCO]       ───────────────────────────             [PRINT]    ─────────────────────
              Γ ⊢ { S1 ... Sn } ok                              Γ ⊢ print(e) ; ok

              Γ(x) = variável/parâmetro
[READ]        ─────────────────────────
              Γ ⊢ read(x) ; ok
```

### Return (função corrente `f`, de retorno `R`)

```
                 Γ ⊢ e : V    V ≼ R    R ≠ void                    R = void
[RETURN-VAL]     ──────────────────────────────     [RETURN-VAZIO] ─────────────────
                 Γ ⊢ return e ; ok                                 Γ ⊢ return ; ok

[CAMINHOS]  R ≠ void  ⇒  todo caminho do corpo de f termina em return:
            return ✓;  bloco ✓ se algum comando ✓;  if-else ✓ se os dois ramos ✓;
            if sem else ✗;  while ✗   (decisão 9)
```

### Chamada

```
             Γ(f) = função (T1, …, Tn) → R    Γ ⊢ ei : Vi    Vi ≼ Ti  (i = 1..n)
[CHAMADA]    ───────────────────────────────────────────────────────────────────
             Γ ⊢ f(e1, …, en) : R

             Γ(f) = função ou procedimento (T1, …, Tn) → R    Γ ⊢ ei : Vi    Vi ≼ Ti
[CHAMADA-CMD] ─────────────────────────────────────────────────────────────────────  (valor descartado, decisão 6)
             Γ ⊢ f(e1, …, en) ; ok
```

Procedimento em expressão é erro (`procedimento 'p' usado em expressão`); aridade diferente é erro (`'f' espera n argumento(s), recebeu m`).

---

## 2. Tabela de operadores

| Operador | Operandos aceitos | Resultado | Recusado (exemplos) |
|---|---|---|---|
| `+` | int/char com int/char | int | bool, string com número |
| `+` | número com double (int, char ou double) | double | |
| `+` | string com string | string | string + int, string + char |
| `-` `*` `/` | int/char com int/char | int | bool, string |
| `-` `*` `/` | número com double | double | |
| `<` `<=` `>` `>=` | número com número | bool | string, bool |
| `==` `!=` | número com número | bool | string com int, bool com int |
| `==` `!=` | bool com bool; string com string | bool | bool com string |
| `&&` `\|\|` | bool com bool | bool | int, string |
| `-` (unário) | int, char → int; double → double | | bool, string |
| `!` | bool | bool | int, string |

"Número" = `int`, `char` ou `double`. Cada linha desta tabela é testada, aceita e recusada, em `test/TypeRulesTest.java`.

---

## 3. As 11 decisões

| # | Decisão | Escolha | Alternativa descartada | Motivo | Teste |
|---|---|---|---|---|---|
| 1 | `int → double` implícito | Sim: em atribuição, inicialização, argumento, return e aritmética mista. O inverso (`double → int`) é sempre erro | Exigir conversão explícita | Não perde informação; o inverso perde a parte decimal | `d01_int_double.txt` |
| 2 | `char` com `int` | `char` vale como `int` em aritmética e comparação; o resultado é `int`. `int → char` é erro, então nunca há valor que "não caiba" | Resultado `char`, com truncamento | Evita estouro silencioso; quem quer `char` usa literal | `d02_char_int.txt` |
| 3 | `bool` em aritmética | Proibido. Condição de `if`/`while` só aceita `bool`; `int` não pode ser condição | `bool` como 0/1, como em C | Pega erros como `if (x = 1)` e `if (n)` | `d03_bool_aritmetica.txt`, `cond_int.txt` |
| 4 | `string`: `==`, `<`, `+` | `==` e `!=` permitidos; `<` etc. proibidos; `+` concatena só `string + string` | Comparação lexicográfica; `+` com conversão automática | A TAM só opera sobre palavras: concatenar e comparar strings já exige rotinas próprias, e ordem lexicográfica não é necessária | `d04_string.txt` |
| 5 | `==` entre tipos diferentes | Números entre si (`int`, `char`, `double`); fora isso, só o mesmo tipo | Aceitar qualquer par | Comparar `bool` com `int` ou `string` com número é quase sempre engano | `d05_igualdade_tipos.txt` |
| 6 | Função usada como comando | Permitido; o valor é descartado | Exigir o uso do valor | Comum (ex.: função com efeito que devolve status) | `d06_funcao_comando.txt` |
| 7 | Recursão mútua | Declaração antecipada: todos os subprogramas de um escopo são pré-declarados antes dos corpos (já era assim no CP4) | Duas passadas completas | Mantém uma passada só e a regra de visibilidade do CP4 | `d07_recursao_mutua.txt` |
| 8 | Argumentos | Aceita o mesmo alargamento da atribuição (`int → double`) | Exigir tipo igual | Consistência: passar argumento é como atribuir ao parâmetro | `d08_argumentos.txt` |
| 9 | `return` em todos os caminhos | Análise de fluxo simples: todo caminho de uma função precisa terminar em `return` (`if` só conta com `else`; `while` não conta) | Basta haver algum `return` | Evita função que "cai do fim" sem valor | `d09_return_caminhos.txt` |
| 10 | Mesmo nome, assinaturas diferentes | Proibido: é redeclaração (um espaço de nomes por escopo, decisão 4.5 do CP4) | Sobrecarga por assinatura | Simplifica identificação e chamada | `d10_sobrecarga.txt` |
| 11 | Nome de função sem parênteses | Erro (`função 'f' usada sem parênteses`); atribuir a uma função também é erro | Tratar como valor (ponteiro de função) | A linguagem não tem tipo função como valor | `d11_nome_sem_parenteses.txt` |

---

## 4. Para a geração de código (registrar, não implementar)

- **Onde converter `int → double`:** em todo ponto onde o código aplica `[ALARGA-DOUBLE]`: atribuição, inicialização, argumento, return e operando `int`/`char` de uma operação `double`. O tipo gravado em `ExprNode.type` diz quando converter (operando `int` dentro de um nó cujo tipo é `double`).
- **`char → int`:** não exige instrução (o caractere já é uma palavra com o código).
- **Representação de `double` na TAM:** **a decidir pelo grupo.** A TAM só tem palavras inteiras. Opções: (a) ponto fixo em uma palavra (ex.: valor × 1000), simples mas com faixa pequena; (b) duas palavras (parte inteira e fração) com rotinas próprias.
- **Representação de `string` na TAM:** **a decidir pelo grupo.** Opções: (a) endereço de uma área com o tamanho seguido dos caracteres, uma palavra por caractere; (b) só literais constantes, sem concatenação em tempo de execução, o que exigiria proibir `+` de strings.
- **Variáveis globais sem inicializador:** ficam fora da análise de "variável sem valor". A geração de código deve inicializá-las com valor padrão (0, `false`, `'\0'`, string vazia).
