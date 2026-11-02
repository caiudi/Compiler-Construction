<div align="center">

**UNIVERSIDADE FEDERAL DE UBERLÂNDIA – UFU**  
**FACULDADE DE COMPUTAÇÃO – FACOM**  
**BACHARELADO EM CIÊNCIA DA COMPUTAÇÃO**

---

# CONSTRUÇÃO DE COMPILADORES

# CHECKPOINT 3 — PROJETO DA ÁRVORE DE SINTAXE ABSTRATA (AST)

**Parte 1 — Projeto da hierarquia de nós**

**Referência:** Gramática final do Checkpoint 2  
**Data:** Novembro de 2026

</div>

---

> **Versão consolidada do projeto da AST.** Baseada na gramática do CP2. O desenho abaixo é textual; a exigência de desenho feito à mão permanece pendente. A autoria e o uso de IA devem ser registrados com transparência no log da entrega.

## 1. Objetivo e decisões

A AST preserva construções com significado para as próximas fases, não todos os símbolos da derivação. As posições `linha:int` e `coluna:int` pertencem à classe abstrata `Node` e representam o **primeiro token da construção**. A hierarquia abaixo define a estrutura a ser usada pelo parser e pelos visitors.

**Extensão planejada para literais `char`:** o CP1 já reconhece `CHAR` como palavra-chave de tipo, mas não reconhece `CHAR_LITERAL` (por exemplo, `'A'`). Para cumprir o escopo de cinco tipos básicos, o projeto passa a prever `CharLiteralNode` e a produção `Primaria → CHAR_LITERAL`. **Esta é uma mudança proposta, ainda não implementada nem validada no scanner/parser originais.** A restrição do CP3 de manter o scanner do CP1 sem modificação precisa ser resolvida com o docente antes de adotá-la na entrega.

A classificação de `NUMERO` em `IntLiteralNode` ou `DoubleLiteralNode` depende de o scanner do CP1 efetivamente aceitar os dois formatos e preservar o lexema; isso deve ser conferido no código original.

## 2. Hierarquia proposta para validação do grupo

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
- Em recuperação de erro, não apresentar uma AST parcial como válida para fases seguintes.

## 5. Programa curto para desenho manual

```text
int calcula(int a) {
    int total = 0;
    while (a > 0) {
        if (a > 2) {
            total = total + a;
        } else {
            total = total + 1;
        }
        a = a - 1;
    }
    return total;
}
```

### Árvore textual completa (o desenho à mão ainda deve ser feito pelo grupo)

```text
Program
  FunctionDecl(calcula)
    Type(INT)
    Id(calcula)
    Param
      Type(INT)
      Id(a)
    Block
      LocalVarStmt
        VarDecl
          Type(INT)
          Id(total)
          IntLiteral(0)
      While
        Binary(>)
          Id(a)
          IntLiteral(0)
        Block
          If
            Binary(>)
              Id(a)
              IntLiteral(2)
            Block
              Assign
                Id(total)
                Binary(+)
                  Id(total)
                  Id(a)
            Block [else]
              Assign
                Id(total)
                Binary(+)
                  Id(total)
                  IntLiteral(1)
          Assign
            Id(a)
            Binary(-)
              Id(a)
              IntLiteral(1)
      Return
        Id(total)
```

Esse exemplo tem mais de 15 nós, uma função, uma seleção e uma repetição. É um **modelo textual**, não substitui a árvore desenhada à mão exigida no enunciado.

### Três trechos da derivação descartados

1. `Expressao → OuLogico → ELogico → Igualdade → Relacional → Aditiva → Multiplicativa → Unaria → Primaria → IDENTIFICADOR` é reduzido a `Id(a)`.
2. `AditivaLinha → MAIS Multiplicativa AditivaLinha` é representado por `Binary(+)`, sem nó `AditivaLinha`.
3. `ComplementoGlobal → ABRE_PARENTESES Parametros FECHA_PARENTESES Bloco` vira uma `FunctionDecl` com lista de parâmetros e corpo, sem nós de parênteses ou de `ComplementoGlobal`.

## 6. Formato proposto para a impressão determinística

- Pré-ordem (visitar o nó antes dos filhos), dois espaços por nível.
- Uma linha por nó; usar o nome do tipo e atributos essenciais, como `Binary(+)`, `Id(total)` e `Type(INT)`.
- Filhos sempre na ordem da gramática e das listas; `else` depois de `then`; argumentos na ordem original.
- Campos opcionais ausentes são impressos como `Else(None)`, `Initializer(None)` ou `ReturnValue(None)` em linha filha. Listas são impressas com cabeçalhos `Parameters(n)`, `Arguments(n)`, `Declarations(n)` e `Statements(n)`, seguidos dos filhos em ordem. A árvore textual da seção 5 é um esboço didático; a saída exata dos testes deve seguir esta convenção, inclusive os cabeçalhos.
- `linha` e `coluna` são omitidas do texto de comparação estrutural, pois não alteram a forma da AST; ficam guardadas no objeto.

## 7. Limites e conferência antes da entrega

A estrutura mapeia os 39 não-terminais da gramática apresentada no CP2. O suporte a `CHAR_LITERAL` está **especificado como extensão**, mas depende de aprovação para alterar o scanner do CP1 e de implementação e testes; não é correto declarar que já funciona na versão original.

A árvore da seção 5 foi construída em texto; **não deve ser descrita como desenho feito à mão**. A atividade pede que o grupo faça esse desenho em papel. Os campos semânticos futuros (símbolo do identificador e tipo inferido da expressão) não são implementados nesta etapa.

A utilização de IA na elaboração deste documento deve ser registrada no `log-uso-ia.md`, conforme as regras efetivas da disciplina.


## 8. Extensão proposta — reconhecimento de literais `char`

### 8.1. Distinção entre tipo e literal

- `CHAR`: palavra-chave `char`, já reconhecida pelo CP1, usada em declarações.
- `CHAR_LITERAL`: valor entre aspas simples, como `'A'`, `'7'`, `'\n'` ou `'\''`, **a acrescentar** ao scanner.
- `CharLiteralNode`: nó de expressão, com `value: char` e posição do token inicial.

### 8.2. Mudanças necessárias no código (não aplicadas nesta versão)

1. Acrescentar `CHAR_LITERAL` ao enum `TokenType`.
2. Acrescentar um reconhecedor de literal de caractere ao `Scanner`, preservando linha e coluna, verificando exatamente um caractere ou uma sequência de escape permitida entre aspas simples. Exemplos válidos: `'a'`, `'0'`, `'\n'`, `'\t'`, `'\\'`, `'\''`. Exemplos inválidos: `''`, `'ab'`, `'x` e `'\q'`.
3. Acrescentar `| CHAR_LITERAL` à produção `Primaria` da gramática.
4. Atualizar o parser: quando o lookahead for `CHAR_LITERAL`, consumir o token e construir `CharLiteralNode` com a posição do token e o valor decodificado.
5. Acrescentar `visitCharLiteral(CharLiteralNode node)` à interface `Visitor<T>` e aos dois visitors. O reimpressor deve escapar corretamente caracteres especiais.

### 8.3. Atualização dos conjuntos

Com a produção `Primaria → CHAR_LITERAL`, acrescentar `CHAR_LITERAL` a `FIRST(Primaria)` e a `FIRST` de todos os níveis que derivam `Primaria`: `Unaria`, `Multiplicativa`, `Aditiva`, `Relacional`, `Igualdade`, `ELogico`, `OuLogico` e `Expressao`; também a `FIRST(Argumentos)` e `FIRST(ValorRetorno)`. O token passa a integrar os conjuntos FOLLOW de `OperadorRelacional` e de outros símbolos que precedem expressões, conforme o cálculo completo. **Os conjuntos FIRST/FOLLOW e a verificação LL(1) devem ser recalculados e publicados em `gramatica-first-follow.md` antes da entrega.**

### 8.4. Testes propostos

```text
char letra = 'A';                // válido após a extensão
char novaLinha = '\n';          // válido após a extensão
char vazio = '';                 // erro léxico
char dois = 'ab';                // erro léxico
char invalido = '\q';          // erro léxico
```

**Compatibilidade com o enunciado:** a exigência expressa de manter o scanner do CP1 sem modificação impede afirmar que esses testes passam no CP3 atual. É necessária autorização do docente para estender o scanner, ou uma orientação alternativa para o caso de `char`.


## 9. Revisão de conformidade com a Parte 1 do enunciado

| Requisito | Local | Situação |
|---|---|---|
| Tabela de nós, atributos tipados e produções | Seção 2 | Documentado |
| Mapeamento de cada não-terminal e justificativas | Seção 3 | Documentado |
| Três classes de auxiliares (precedência, recursão à esquerda, fatoração) | Seção 3 | Documentado |
| Descarte de pontuação e delimitadores | Seção 3 | Documentado |
| Linha e coluna do primeiro token | Seção 4 | Especificado; confirmar se o token do CP1 guarda coluna |
| Exemplo com função, seleção, repetição e mais de 15 nós | Seção 5 | Esboço textual presente; desenho manual pendente |
| Três trechos de derivação descartados | Seção 5 | Documentado |
| Extensibilidade para símbolo e tipo semântico | Seção 4 | Documentado sem implementação |
| Impressão determinística para testes futuros | Seção 6 | Especificada; alinhar a saída exata dos visitors |
| Literais `char` | Seção 8 | **Pendente:** scanner original não os reconhece; depende de autorização e revisão de CP1/CP2 |

**Conferências antes de entregar:** (1) conferir as assinaturas e os nomes reais de tokens no código do CP1; (2) desenhar manualmente a AST e anexar foto ou desenho feito pelo grupo; (3) não afirmar que `CHAR_LITERAL` já funciona; (4) assegurar que a saída de exemplo e o visitor futuro sigam exatamente a mesma convenção; (5) registrar o uso de IA no log.
