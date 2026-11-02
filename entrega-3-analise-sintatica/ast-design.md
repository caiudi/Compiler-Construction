<div align="center">

**UNIVERSIDADE FEDERAL DE UBERLÂNDIA – UFU**  
**FACULDADE DE COMPUTAÇÃO – FACOM**  
**BACHARELADO EM CIÊNCIA DA COMPUTAÇÃO**

---

# CONSTRUÇÃO DE COMPILADORES

# CHECKPOINT 3 — PROJETO DA ÁRVORE DE SINTAXE ABSTRATA (AST)

**Parte 1 — Projeto da hierarquia de nós**

**Data:** Novembro de 2026

</div>

---

## 1. Objetivo e decisões

A AST preserva construções com significado para as próximas fases, não todos os símbolos da derivação. As posições `linha:int` e `coluna:int` pertencem à classe abstrata `Node` e representam o **primeiro token da construção**. A hierarquia abaixo define a estrutura a ser usada pelo parser e pelos visitors.

## 2. Hierarquia proposta

Todos os nós herdam `Node(linha:int, coluna:int)` e futuramente implementarão `accept(Visitor<T>)`.

| Nó concreto | Atributos adicionais (tipos) | Produções de origem |
|---|---|---|
| `ProgramNode` | `declarations: List<DeclNode>` | `Programa`, `ListaDeclaracoes` |
| `VarDeclNode` | `type: TypeNode`, `name: IdNode`, `initializer: ExprNode?` | `DeclaracaoGlobal`, `DeclaracaoLocal`, `ComplementoGlobal`, `Inicializacao` |
| `LocalVarStmtNode` | `declaration: VarDeclNode` | `DeclaracaoLocal` em `Comando` |
| `FunctionDeclNode` | `returnType: TypeNode`, `name: IdNode`, `parameters: List<ParamNode>`, `body: BlockNode` | `DeclaracaoGlobal`, `ComplementoGlobal`, `Parametros`, `Bloco` |
| `ProcedureDeclNode` | `name: IdNode`, `parameters: List<ParamNode>`, `body: BlockNode` | `DeclaracaoGlobal` com `VOID` |
| `ParamNode` | `type: TypeNode`, `name: IdNode` | `Parametro` |
| `TypeNode` | `kind: BasicType` (`INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING`) | `Tipo` |
| `BlockNode` | `statements: List<StmtNode>` | `Bloco`, `ListaComandos` |
| `AssignNode` | `target: IdNode`, `value: ExprNode` | `ComandoIdentificador`, `ComplementoIdentificador` |
| `IfNode` | `condition: ExprNode`, `thenBlock: BlockNode`, `elseBlock: BlockNode?` | `ComandoIf`, `OpcaoElse` |
| `WhileNode` | `condition: ExprNode`, `body: BlockNode` | `ComandoWhile` |
| `CallStmtNode` | `call: CallExprNode` | `ComandoIdentificador` com chamada |
| `ReturnNode` | `value: ExprNode?` | `ComandoRetorno`, `ValorRetorno` |
| `IntLiteralNode` | `lexeme: String` | `Primaria → NUMERO` (inteiro, após classificar lexema) |
| `DoubleLiteralNode` | `lexeme: String` | `Primaria → NUMERO` (decimal, após classificar lexema) |
| `BoolLiteralNode` | `value: boolean` | `Primaria → TRUE \| FALSE` |
| `StringLiteralNode` | `lexeme: String` | `Primaria → STRING_LITERAL` |
| `CharLiteralNode` | `value: char` | `Primaria → CHAR_LITERAL` **(extensão proposta; requer atualização léxica)** |
| `IdNode` | `name: String` | `IDENTIFICADOR` em declarações e expressões |
| `BinaryExprNode` | `operator: TokenType`, `left: ExprNode`, `right: ExprNode` | `OuLogicoLinha`, `ELogicoLinha`, `IgualdadeLinha`, `RelacionalLinha`, `AditivaLinha`, `MultiplicativaLinha` |
| `UnaryExprNode` | `operator: TokenType`, `operand: ExprNode` | `Unaria` |
| `CallExprNode` | `callee: IdNode`, `arguments: List<ExprNode>` | `Primaria`, `SufixoIdentificador`, `Argumentos` |

**Hierarquia abstrata:** `Node` é a raiz; `DeclNode`, `StmtNode` e `ExprNode` herdam de `Node`. `ProgramNode`, `ParamNode` e `TypeNode` herdam diretamente de `Node`. `VarDeclNode`, `FunctionDeclNode` e `ProcedureDeclNode` herdam de `DeclNode`. `LocalVarStmtNode`, `BlockNode`, `AssignNode`, `IfNode`, `WhileNode`, `CallStmtNode` e `ReturnNode` herdam de `StmtNode`. Literais, `IdNode`, `BinaryExprNode`, `UnaryExprNode` e `CallExprNode` herdam de `ExprNode`.

**Decisões de representação propostas:** declarações locais usam `LocalVarStmtNode` envolvendo `VarDeclNode`, evitando herança múltipla em Java; nomes declarados usam `IdNode`; `NUMERO` é classificado em `IntLiteralNode` ou `DoubleLiteralNode` pela presença de separador decimal ou expoente no lexema, mantendo o texto original; `CharLiteralNode` representa um caractere decodificado, condicionado à extensão léxica `CHAR_LITERAL`. `CallStmtNode` diferencia uma chamada usada como comando de uma chamada usada como expressão.

## 3. Mapeamento de todos os não-terminais

| Não-terminal do CP2 | Destino na AST | Justificativa |
|---|---|---|
| `Programa` | `ProgramNode` | Raiz da unidade compilada. |
| `ListaDeclaracoes` | atributo `ProgramNode.declarations` | Lista recursiva é estrutura de agrupamento. |
| `Tipo` | `TypeNode` | Tipo declarado importa semanticamente. |
| `DeclaracaoGlobal` | `VarDeclNode`, `FunctionDeclNode` ou `ProcedureDeclNode` | A escolha depende do complemento e de `VOID`. |
| `ComplementoGlobal` | nenhum nó próprio | Auxiliar de fatoração; determina variável ou função. |
| `DeclaracaoLocal` | `LocalVarStmtNode` contendo `VarDeclNode` | Representa declaração em bloco sem herança múltipla. |
| `Inicializacao` | atributo `VarDeclNode.initializer` | `ε` significa inicializador ausente. |
| `Parametros` | atributo `parameters` | Coleção, não categoria sintática independente. |
| `Parametro` | `ParamNode` | Parâmetro individual possui tipo e nome. |
| `ListaParametros` | atributo `parameters` | Auxiliar recursivo de lista. |
| `Bloco` | `BlockNode` | Delimita escopo de comandos. |
| `ListaComandos` | atributo `BlockNode.statements` | Lista, não nó individual. |
| `Comando` | nenhum nó próprio | Alternativas já têm nós específicos. |
| `ComandoIdentificador` | `AssignNode` ou `CallStmtNode` | Fatoração do prefixo identificador. |
| `ComplementoIdentificador` | nenhum nó próprio | Auxiliar de fatoração: `=` ou `(` escolhe o nó. |
| `ComandoRetorno` | `ReturnNode` | Comando com valor opcional. |
| `ValorRetorno` | atributo `ReturnNode.value` | `ε` corresponde a `null`. |
| `ComandoWhile` | `WhileNode` | Repetição. |
| `ComandoIf` | `IfNode` | Seleção. |
| `OpcaoElse` | atributo `IfNode.elseBlock` | `ε` significa ausência de `else`. |
| `Argumentos` | atributo `CallExprNode.arguments` | Coleção de expressões. |
| `ListaArgumentos` | atributo `CallExprNode.arguments` | Auxiliar recursivo de lista. |
| `Expressao` | nenhum nó próprio | Encaminha para expressão de menor precedência. |
| `OuLogico` | `ExprNode` resultante | Nível de precedência, sem wrapper. |
| `OuLogicoLinha` | `BinaryExprNode` por operador | Auxiliar da eliminação da recursão à esquerda. |
| `ELogico` | `ExprNode` resultante | Nível de precedência. |
| `ELogicoLinha` | `BinaryExprNode` por operador | Auxiliar da eliminação da recursão à esquerda. |
| `Igualdade` | `ExprNode` resultante | Nível de precedência. |
| `IgualdadeLinha` | `BinaryExprNode` por operador | Auxiliar da eliminação da recursão à esquerda. |
| `Relacional` | `ExprNode` resultante | Nível de precedência. |
| `RelacionalLinha` | `BinaryExprNode` por operador | Auxiliar da eliminação da recursão à esquerda. |
| `OperadorRelacional` | atributo `BinaryExprNode.operator` | Operador, não nó próprio. |
| `Aditiva` | `ExprNode` resultante | Nível de precedência. |
| `AditivaLinha` | `BinaryExprNode` por operador | Auxiliar da eliminação da recursão à esquerda. |
| `Multiplicativa` | `ExprNode` resultante | Nível de precedência. |
| `MultiplicativaLinha` | `BinaryExprNode` por operador | Auxiliar da eliminação da recursão à esquerda. |
| `Unaria` | `UnaryExprNode` ou expressão primária | Só há nó quando há operador unário. |
| `Primaria` | literal, `IdNode`, `CallExprNode` ou expressão interna | Parênteses não acrescentam nó. |
| `SufixoIdentificador` | `CallExprNode` ou nada | Auxiliar de fatoração para distinguir `id` de `id(...)`. |

**Três categorias exigidas na Aula 7:** (a) não-terminais de precedência, como `Aditiva`, `Multiplicativa` e `Primaria`, não precisam de nó wrapper; (b) auxiliares da eliminação de recursão à esquerda, como `AditivaLinha`, geram nós `BinaryExprNode` encadeados, não nós com o nome do auxiliar; (c) auxiliares de fatoração, como `ComplementoGlobal` e `ComplementoIdentificador`, apenas determinam a construção concreta.

**Tokens descartados:** `ABRE_PARENTESES`, `FECHA_PARENTESES`, `ABRE_CHAVES`, `FECHA_CHAVES`, `VIRGULA`, `PONTO_VIRGULA` e `EOF` não viram nós; são delimitadores de sintaxe. Os operadores não viram nós separados, mas são preservados em `operator`.

## 4. Posições e evolução futura

- Cada nó registra `linha` e `coluna` do primeiro token da construção. Para uma expressão binária, o nó da operação completa começa no primeiro token do operando esquerdo, **não** na posição do operador.
- Para um `IfNode`, a posição é a de `IF`; para `VarDeclNode`, a posição é a do token de tipo; para `CallExprNode`, a posição é a do identificador da chamada.
- Mais adiante, `IdNode` poderá receber referência para símbolo e `ExprNode` poderá receber um tipo inferido. **Esses campos não são implementados no CP3.**
- Em recuperação de erro, não apresentar uma AST parcial como válida para fases seguintes
