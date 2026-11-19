# Log de Uso de Inteligência Artificial — Checkpoint 5

> **A completar/revisar pelo grupo:** acrescentem outras ferramentas que usarem nesta etapa (ChatGPT, Gemini etc.) e confirmem que entendem o código abaixo. Ele será cobrado na defesa oral.

## Claude (Anthropic), via Claude Code — novembro de 2026

**Decisões tomadas pelo grupo** quando perguntado:
- estender a linguagem com `print` e `read`;
- estender a linguagem com funções/procedimentos aninhados;
- adotar a proposta da IA para as 11 decisões de tipos (registradas em `regras-de-tipos-rascunho.md`, Seção 3), podendo mudá-las depois.

**Gerado pela IA:**
- Extensão léxica e sintática: palavras reservadas `print`/`read`; `ComandoPrint`, `ComandoRead` e `DeclaracaoLocal` com subprogramas no parser; nós `PrintNode`, `ReadNode`, `LocalSubprogramStmtNode`; métodos novos no `Visitor`, `AstPrinter` e `SourcePrinter`; `gramatica-first-follow.md` com FIRST/FOLLOW e LL(1) refeitos só para os não-terminais afetados.
- `VisitorBase` (visitor que não faz nada, para os visitors auxiliares).
- `IdentificationVisitor`: preenchimento de tipo e assinatura no `Symbol`; pré-declaração dos subprogramas de cada bloco; inicializador resolvido antes de declarar o nome.
- Pacote `types` (`Type`, `Signature`), campo `ExprNode.type` e campos `type`/`signature` em `Symbol`.
- `TypeChecker` (verificador), `Analisador` (encadeamento das fases) e `Main`.
- Testes: `TypeRulesTest`, `TypeSuiteTest`, `RobustnessTest`, `ExtensaoSintaxeTest` e os 24 programas de `test/programas/` com as saídas de `test/esperado/`. As saídas esperadas foram escritas a partir das regras antes de rodar o verificador, e o teste foi conferido falhando com uma saída esperada alterada de propósito.
- Reorganização dos testes de regressão em `test/cp3/` e `test/cp4/`.
- `README.md` e `regras-de-tipos-rascunho.md`.

**Não feito pela IA:** a Parte 1 (`tipos-a-mao.pdf`), que precisa ser escrita à mão pelo grupo.
