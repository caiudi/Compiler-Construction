# Gramática revisada — Checkpoint 5

O enunciado do CP5 pede que o verificador confira os comandos `print` e `read` e que haja teste de **função aninhada usando variável do nível acima**. A linguagem até o CP4 não tinha nenhum dos dois, então o grupo estendeu a linguagem. Este documento registra **só o que mudou** em relação à gramática do CP2 (`entrega-2-analisador-sintatico/gramatica-first-follow.pdf`) e à revisão do CP3 (`CHAR_LITERAL`, em `entrega-3-analise-sintatica/gramatica-first-follow.md`).

## 1. Mudança léxica

Duas palavras reservadas novas, `print` e `read` (tokens `PRINT` e `READ`), acrescentadas à tabela de palavras reservadas do scanner. Como o scanner já reconhece palavras reservadas pelo AFD de identificador seguido de busca na tabela, nenhum AFD mudou. `print` e `read` deixam de ser identificadores válidos.

## 2. Mudança na gramática

Antes (CP2/CP3):

```text
Comando        → DeclaracaoLocal | ComandoIdentificador | ComandoRetorno
               | ComandoWhile | ComandoIf | Bloco
DeclaracaoLocal → Tipo IDENTIFICADOR Inicializacao PONTO_VIRGULA
```

Depois (CP5):

```text
Comando        → DeclaracaoLocal | ComandoIdentificador | ComandoRetorno
               | ComandoWhile | ComandoIf | Bloco
               | ComandoPrint | ComandoRead
DeclaracaoLocal → Tipo IDENTIFICADOR ComplementoGlobal
               | VOID IDENTIFICADOR ABRE_PARENTESES Parametros FECHA_PARENTESES Bloco
ComandoPrint   → PRINT ABRE_PARENTESES Expressao FECHA_PARENTESES PONTO_VIRGULA
ComandoRead    → READ ABRE_PARENTESES IDENTIFICADOR FECHA_PARENTESES PONTO_VIRGULA
```

`DeclaracaoLocal` passa a ter **as mesmas formas da declaração global**: reaproveita `ComplementoGlobal` (que escolhe entre `= expr ;`, `;` e `( params ) Bloco`) e a alternativa de procedimento com `VOID`. Assim uma função ou procedimento pode ser declarado dentro de qualquer bloco. Nenhuma outra produção mudou.

## 3. Efeito sobre FIRST e FOLLOW

| Não-terminal | Mudança |
|---|---|
| DeclaracaoLocal | FIRST ganha `VOID` |
| Comando, ListaComandos | FIRST ganha `VOID`, `PRINT`, `READ` |
| ComandoPrint (novo) | FIRST = {`PRINT`}; FOLLOW = FOLLOW(Comando) |
| ComandoRead (novo) | FIRST = {`READ`}; FOLLOW = FOLLOW(Comando) |
| Comando, DeclaracaoLocal, ComandoIdentificador, ComplementoIdentificador, ComandoRetorno, ComandoWhile, ComandoIf, OpcaoElse | FOLLOW ganha `VOID`, `PRINT`, `READ` (vêm de FIRST(ListaComandos)) |
| Bloco | FOLLOW ganha `PRINT`, `READ` (`VOID` já estava) |
| ComplementoGlobal | FOLLOW ganha FOLLOW(DeclaracaoLocal) = FOLLOW(Comando) |
| Expressao e seus níveis | sem mudança: em `print(...)` a expressão é seguida de `FECHA_PARENTESES`, que já estava no FOLLOW |

FOLLOW(Comando) no CP5 = {`ABRE_CHAVES`, `BOOL`, `CHAR`, `DOUBLE`, `FECHA_CHAVES`, `IDENTIFICADOR`, `IF`, `INT`, `PRINT`, `READ`, `RETURN`, `STRING`, `VOID`, `WHILE`}.

## 4. Verificação LL(1) refeita

| Não-terminal | Verificação | Resultado |
|---|---|---|
| Comando | FIRST das 8 alternativas: {tipos, `VOID`}, {`IDENTIFICADOR`}, {`RETURN`}, {`WHILE`}, {`IF`}, {`ABRE_CHAVES`}, {`PRINT`}, {`READ`}: disjuntos dois a dois; nenhuma anulável | LL(1) |
| DeclaracaoLocal | {`INT`,`DOUBLE`,`BOOL`,`CHAR`,`STRING`} ∩ {`VOID`} = ∅; nenhuma anulável | LL(1) |
| ComplementoGlobal | {`ATRIBUICAO`, `PONTO_VIRGULA`} ∩ {`ABRE_PARENTESES`} = ∅; nenhuma anulável. Ganhou FOLLOW, mas não é anulável, então o FOLLOW não entra na decisão | LL(1) |
| ComandoPrint, ComandoRead | uma alternativa cada | LL(1) |
| ListaComandos | FIRST(Comando) ∩ FOLLOW(ListaComandos) = FIRST(Comando) ∩ {`FECHA_CHAVES`} = ∅ | LL(1) |
| OpcaoElse | {`ELSE`} ∩ FOLLOW(OpcaoElse) = ∅ (`ELSE` não está entre os tokens acrescentados) | LL(1) |

Os demais não-terminais anuláveis (`Inicializacao`, `Parametros`, `ListaParametros`, `ValorRetorno`, `Argumentos`, `ListaArgumentos`, os `…Linha` e `SufixoIdentificador`) não tiveram FIRST nem FOLLOW alterados. **A gramática continua LL(1).**

## 5. Impacto no parser e na AST

- `Parser.comando()`: `INICIO_COMANDO` ganha `VOID`, `PRINT`, `READ`; novos ramos para `print` e `read`; tipos e `void` vão para `declaracaoLocal()`.
- `Parser.global()` foi dividido em `variavel()`, `funcao()` e `procedimento()`, usados pelo nível global e pelos blocos.
- Três nós novos: `PrintNode(value)`, `ReadNode(target)` e `LocalSubprogramStmtNode(declaration)`. Este último envolve o mesmo `FunctionDeclNode`/`ProcedureDeclNode` do nível global, como `LocalVarStmtNode` envolve `VarDeclNode`. O `Visitor` ganhou `visitPrint`, `visitRead` e `visitLocalSubprogram`.
- A recuperação em modo pânico não mudou. As suítes do CP2 (`ParserTest`), do CP3 (`ASTTest`, `CharLiteralTest`) e do CP4 continuam passando, e o `ExtensaoSintaxeTest` cobre a sintaxe nova.

## 6. Identificação (CP4) com subprogramas aninhados

Aplica a decisão 4.2 do CP4 ("nomes de funções/procedimentos são pré-declarados no escopo que os contém antes da visita dos corpos") também aos blocos:

1. Ao entrar em um bloco, os subprogramas declarados nele são pré-declarados no escopo do bloco. Isso permite recursão e recursão mútua entre irmãos.
2. Os comandos são visitados na ordem. O corpo do subprograma aninhado abre o escopo dos parâmetros por cima do escopo do bloco, e por isso enxerga as variáveis do nível de cima declaradas **antes** dele.
3. Fora do bloco, o subprograma não é visível (`test/programas/aninhada_fora_do_escopo.txt`).

> **Para a geração de código:** acessar uma variável do nível de cima exige encadeamento estático (static link) na TAM. O `Symbol.level` do CP4 já guarda o nível de cada declaração.
