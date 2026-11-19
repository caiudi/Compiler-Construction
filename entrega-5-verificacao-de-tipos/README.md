# Checkpoint 5 — Verificador de tipos

## Entregáveis

| Item | Onde | Situação |
|---|---|---|
| Parte 1 — regras à mão | `tipos-a-mao.pdf` | **A fazer pelo grupo, à mão.** Rascunho em `regras-de-tipos-rascunho.md` |
| Parte 2 — verificador | `src/` | Pronto |
| Parte 3 — testes | `test/programas/` + `test/esperado/`, `test/*Test.java` | Pronto |
| Extensão da linguagem | `gramatica-first-follow.md` | `print`, `read` e subprogramas aninhados |
| Log de IA | `log-uso-ia.md` | Revisar |

## Como os tipos estão representados

- **`types.Type`** (`src/types/Type.java`): um objeto único por tipo (`INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING`, `VOID`, `ERROR`), comparado com `==`. `VOID` é o retorno de procedimento. `ERROR` é o tipo de uma expressão que já deu erro e é compatível com tudo, o que evita a cascata.
- **`types.Signature`** (`src/types/Signature.java`): tipos dos parâmetros e tipo de retorno de uma função ou procedimento.
- **`Symbol`** (`src/symbols/Symbol.java`) ganhou `type` e `signature`, preenchidos pela **identificação** (CP4) ao declarar cada nome. O verificador **só lê** esses campos por `IdNode.decl`: não declara nomes nem abre ou fecha escopos.
- **`ExprNode.type`** (`src/ast/ExprNode.java`): o tipo calculado para cada expressão, que a geração de código vai ler. Na hierarquia do CP4, essa é a única mudança feita pelo verificador. Os nós novos (`PrintNode`, `ReadNode`, `LocalSubprogramStmtNode`) são da extensão da linguagem, não do verificador.

## Onde fica cada regra

Tudo em `src/visitor/TypeChecker.java`, um `Visitor<Type>` sem `instanceof` nem `switch` sobre o tipo do nó. Há `switch` apenas sobre o enum do operador.

| Regra (rascunho, Seção 1) | Método |
|---|---|
| `[LIT-*]` | `visitIntLiteral` … `visitCharLiteral` |
| `[ID]`, `[ID-SUBPROG]` | `visitId` |
| `[COMPAT]`, `[ALARGA-*]` | `assignable` |
| `[ARIT-INT]`, `[ARIT-DOUBLE]`, `[CONCAT]`, `[REL]`, `[IGUAL-*]`, `[LOG]` | `visitBinaryExpr` → `binaryResult` / `arithmetic` |
| `[NEG-*]`, `[NAO]` | `visitUnaryExpr` → `unaryResult` |
| `[ERRO-BIN]`, `[ERRO-UN]` | início de `visitBinaryExpr` / `visitUnaryExpr` |
| `[ATRIB]` | `visitAssign` + `target` |
| `[DECL]` | `visitVarDecl` (a identificação resolve o inicializador antes de declarar o nome) |
| `[IF]`, `[WHILE]` | `visitIf`, `visitWhile`, `condition` |
| `[RETURN-*]` | `visitReturn` |
| `[CAMINHOS]` | `visitFunctionDecl` + `RETURN_PATHS` |
| `[CHAMADA]`, `[CHAMADA-CMD]` | `call` (de `visitCallExpr` e `visitCallStmt`) |
| `[PRINT]`, `[READ]` | `visitPrint`, `visitRead` |

| Decisão | Onde |
|---|---|
| 1, 2, 8 (`int → double`, `char` como `int`, argumentos) | `assignable`, `arithmetic` |
| 3 (`bool` fora da aritmética, condição só `bool`) | `arithmetic`, `condition` |
| 4, 5 (`string`, `==` entre tipos) | `binaryResult` |
| 6 (função como comando) | `visitCallStmt` → `call(…, true)` |
| 7 (recursão mútua) | `IdentificationVisitor`: `predeclare` / `predeclareLocal` |
| 9 (`return` em todos os caminhos) | `RETURN_PATHS` |
| 10 (sobrecarga proibida) | `IdentificationVisitor.declare` (redeclaração) |
| 11 (nome de função sem parênteses) | `visitId`, `target` |

**Contexto:** a pilha `subprograms` guarda o subprograma corrente para conferir o `return`. `openLoops` conta os laços abertos; a linguagem não tem `break`, então ele ainda não é consultado.

**Variável sem valor (aviso):** `Flow` guarda as variáveis locais que certamente já receberam valor no ponto atual. Um `if` fica com a interseção dos dois ramos; o que é atribuído dentro de um `while` não conta depois dele; depois de um `return` o caminho está morto. Só variáveis **locais declaradas sem inicializador** são acompanhadas, e sai no máximo um aviso por variável. Globais ficam de fora porque a ordem das chamadas não é conhecida; a geração de código deve zerá-las.

## Fases e mensagens

`src/Analisador.java` encadeia sintaxe → identificação → tipos. Com erro sintático, para. Com erro de identificação, o verificador de tipos roda mesmo assim: o nome não declarado vira `erro`, sem nova mensagem. Com qualquer erro, o programa **não segue para a geração de código**. Avisos não bloqueiam.

Saída: erros de identificação e de tipos intercalados por linha (na mesma linha, os de identificação primeiro), depois os avisos.

```text
linha L: operador '+' não se aplica a int e bool
linha L: operador '!' não se aplica a int
linha L: condição de 'if' deve ser bool, mas é int
linha L: atribuição a 'x' espera int, mas recebeu double
linha L: inicialização de 'x' espera int, mas recebeu double
linha L: 'f' retorna int, mas o return devolve string
linha L: return sem valor em 'f', que retorna int
linha L: procedimento 'p' não pode retornar valor
linha L: função 'f' pode terminar sem return
linha L: 'fat' espera 1 argumento(s), recebeu 2
linha L: argumento 1 de 'mostra' esperado string, veio int
linha L: procedimento 'p' usado em expressão
linha L: 'x' não é função nem procedimento
linha L: função 'f' usada sem parênteses
linha L: 'f' não é variável
linha L: aviso: variável 'x' pode ser usada sem valor
```

## Testes

| Classe | O que cobre |
|---|---|
| `TypeRulesTest` | Cada linha da tabela de operadores, aceita e recusada, mais a compatibilidade e a ausência de cascata, montando a AST à mão, **sem parser** (142 verificações) |
| `TypeSuiteTest` | Os 24 programas de `test/programas/` contra `test/esperado/`; em programa sem erros, confere que toda expressão ficou com tipo |
| `RobustnessTest` | Cada prefixo de cada programa de teste e versões com um caractere removido (~6.000 entradas): nenhuma exceção nem laço infinito |
| `ExtensaoSintaxeTest` | `print`, `read` e funções aninhadas no parser, na impressora e na ida e volta |
| `ParserTest`, `ASTTest`, `CharLiteralTest` | Regressão do CP2 e do CP3 (`test/cp3/esperado/`) |
| `IdentificationSuiteTest`, `IdentificationTest`, `SymbolTable*Test` | Regressão do CP4 (`test/cp4/`) |

Programas obrigatórios: `valido_completo`, `erro_expressao`, `cond_int`, `return_incompativel`, `chamada_errada` (aridade e tipo), `procedimento_expressao`, `varios_erros`, `recursiva`, `d07_recursao_mutua`, `aninhada` (usa variável do nível de cima). Um programa por decisão: `d01` a `d11`. Extras: `aninhada_fora_do_escopo`, `aviso_sem_valor`, `inicializador_antes_do_nome` e `print_read`.

## Compilar e testar (a partir desta pasta)

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src,test -Filter *.java).FullName
java -cp out TypeRulesTest
java -cp out TypeSuiteTest
java -cp out RobustnessTest 2>$null
java -cp out ExtensaoSintaxeTest
java -cp out ParserTest
java -cp out ASTTest
java -cp out CharLiteralTest
java -cp out IdentificationSuiteTest
java -cp out IdentificationTest
java -cp out SymbolTableTest
java -cp out SymbolTableContractTest
```

Analisar um programa: `java -cp out Main test/programas/varios_erros.txt` (acrescente `--ast` para imprimir a árvore de um programa sem erros). Os arquivos devem estar em UTF-8 sem BOM.

## Limites conhecidos

- Não há `break`. O contexto já conta os laços abertos para quando houver.
- Nenhuma verificação de valores, como divisão por zero: só tipos.
- O nome chamado em `f(...)` (`CallExprNode.callee`) fica sem `type`; o tipo da chamada está no próprio `CallExprNode`.
