# Your Finances

App Android pessoal de controle financeiro: entradas, saídas fixas, saídas flexíveis e dinheiro guardado. Local-first, privado e open source (GPL-3.0). Uso inicial pessoal, mas o código deve estar pronto para publicação no F-Droid.

## Princípios inegociáveis

- **Local-first:** todos os dados ficam no aparelho. Não existe backend, conta de usuário nem nuvem.
- **Privacidade:** zero telemetria, analytics ou crash reporting. Nada de Firebase, Google Play Services, Crashlytics ou SDKs de terceiros que façam rede.
- **Sem rede:** o app não faz nenhuma requisição de rede e não pede a permissão `INTERNET`. (A ideia de estimar rendimentos pela API do Banco Central foi descartada: cada banco e produto rende diferente, e o número precisaria ser corrigido à mão.)
- **Compatível com F-Droid:** apenas dependências open source, builds reproduzíveis, nenhum blob proprietário.
- **Os dados são do usuário:** sempre deve existir exportação em CSV aberto, além do backup criptografado.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Single-Activity com Navigation Compose
- Room sobre SQLCipher (`net.zetetic:sqlcipher-android`); chave do banco protegida pelo Android Keystore
- Koin para injeção de dependência
- WorkManager para tarefas em segundo plano (previsão de recorrências)
- kotlinx.serialization (JSON) para o conteúdo do backup
- `androidx.biometric` para bloqueio; `FLAG_SECURE` em todas as telas
- Storage Access Framework para backup/exportação (sem permissão de armazenamento)
- Testes: JUnit 5 + kotlinx-coroutines-test; Turbine para Flows

Antes de adicionar qualquer dependência nova, confirme que é open source, sem Play Services e compatível com F-Droid. Na dúvida, pergunte.

## Arquitetura

```
app/
  data/      Room (entities, DAOs), SQLCipher, backup, repositórios
  domain/    Regras financeiras em Kotlin puro (SEM imports de Android)
  ui/        Telas Compose e ViewModels (MVVM, estado exposto via StateFlow)
```

- A camada `domain` não depende de Android. Toda regra de cálculo mora lá e tem teste unitário.
- ViewModels não fazem cálculo financeiro; delegam para casos de uso do `domain`.
- Repositórios expõem `Flow`; a UI observa.

## Regras de domínio

### Dinheiro
- Valores **sempre em centavos como `Long`**. Nunca usar `Double`/`Float` para dinheiro.
- Moeda única: BRL. Formatação só na camada de UI (`pt-BR`).
- Divisões (ex.: parcelas) devem distribuir os centavos restantes de forma determinística (o resto vai na primeira parcela) e a soma deve bater exatamente com o total.

### Datas
- Usar `java.time` (`LocalDate`, `YearMonth`). Nada de `java.util.Date`.

### Lançamentos
Tipos: `INCOME` (salário, renda extra), `FIXED_EXPENSE` (assinaturas, contas, parcelas), `FLEXIBLE_EXPENSE` (jantar, roupa, cinema), `INVESTMENT` (aporte), `WITHDRAWAL` (resgate do que foi guardado).

Cada lançamento tem duas datas:
- `occurredOn` (competência): quando a compra/evento aconteceu.
- `paidOn` (caixa): quando o dinheiro saiu ou entrou. Para cartão de crédito, é a data de vencimento da fatura.

Relatórios precisam deixar claro se usam competência ou caixa.

### Recorrências
- Uma `Recurrence` é um modelo (valor, dia, frequência, categoria) que gera lançamentos com status `PREDICTED`.
- O usuário confirma (`CONFIRMED`) ou ajusta o valor real. Editar uma ocorrência não altera o modelo, a menos que o usuário escolha "aplicar às próximas".

### Parcelas
- Uma compra parcelada gera N lançamentos vinculados à compra original, com `installmentNumber` e `installmentCount`.
- A competência (`occurredOn`) de cada parcela é a data em que ela vence, para o orçamento mensal refletir o peso de cada mês. A data real da compra fica em `InstallmentPurchase.purchasedOn`.
- O app deve conseguir responder: quando cada parcelamento termina e quanto do futuro já está comprometido por mês.

### Categorias e orçamento
- Categorias são de entrada (`INCOME`) ou de saída (`EXPENSE`); aportes e resgates usam caixinhas, não categorias.
- Banco novo começa com uma lista padrão de categorias, que o usuário edita ou exclui. Excluir deixa os lançamentos sem categoria.
- Categorias de saída podem ter limite mensal: é só referência nos relatórios, não altera o "disponível".
- Relatórios usam competência, como a tela principal, e incluem saídas previstas.

### Importação de extrato
- OFX (1.x SGML e 2.x XML, de conta e de cartão) e CSV com mapeamento de colunas, que fica lembrado por cabeçalho.
- Valores e datas são lidos sem ponto flutuante; datas com o dia primeiro (padrão brasileiro) ou ISO.
- Saída vira flexível, entrada vira entrada; o usuário revisa tipo, categoria e seleção antes de salvar, tudo ou nada.
- Cada linha importada guarda uma chave (`importKey`): o FITID do banco ou data, valor e descrição numerados. Linha já importada ou igual a um lançamento digitado à mão começa desmarcada.
- No extrato de cartão, as compras entram nas faturas; entradas (pagamento da fatura, estornos) começam desmarcadas.

### Cartão de crédito
- Cada cartão tem dia de fechamento e dia de vencimento.
- Compra feita após o fechamento cai na fatura do mês seguinte.
- A fatura é a soma dos lançamentos daquele ciclo; seu pagamento é o evento de caixa.
- Recorrências de saída podem ser no cartão: cada ocorrência entra na fatura em que cair.

### "Quanto ainda posso gastar" (tela principal)
```
disponível = entradas do mês
           + resgates confirmados do mês
           − saídas fixas do mês (previstas + confirmadas)
           − o maior entre a meta de investimento e os aportes do mês (previstos + confirmados)
           − saídas flexíveis já registradas
```
Este é o número mais importante do app. Qualquer mudança nessa regra exige teste.
A meta reserva o dinheiro antes mesmo do aporte; aportar além dela reserva mais. Resgates previstos só contam depois de confirmados. A tela principal usa competência e compara "meta" com "guardado no mês".

### Dinheiro guardado
- O app registra só o que foi guardado, nunca rendimentos: eles dependem de cada banco e produto. O saldo real se confere no banco.
- Aportes (`INVESTMENT`) e resgates (`WITHDRAWAL`), inclusive recorrentes, podem indicar uma caixinha (`SavingsPocket`: nome e objetivo opcional) ou nenhuma ("sem caixinha").
- Guardado = aportes − resgates, contando só lançamentos `CONFIRMED` com data até hoje. Pode ficar negativo se mais foi resgatado do que registrado.
- A meta mensal de investimento vale a partir de um mês até ser substituída; mudá-la não altera os meses anteriores.
- Caixinha com aportes, resgates ou recorrências não pode ser excluída.

## Segurança

- Banco sempre criptografado com SQLCipher desde o primeiro commit com dados.
- Bloqueio por biometria/PIN ao abrir e ao voltar do background.
- Backup: arquivo exportado criptografado com senha definida pelo usuário.
- Nunca logar valores, descrições de lançamentos ou qualquer dado financeiro (nem em debug).
- `android:allowBackup="false"` para evitar backup automático na nuvem do Google.

## Convenções de código

- Código, nomes de classes, variáveis e commits em **inglês**.
- Textos da interface em **português (pt-BR)**, sempre via `strings.xml`, nunca hardcoded.
- Seguir o estilo oficial do Kotlin; ktlint/detekt quando configurados.
- Funções do `domain` puras sempre que possível.
- Commits pequenos e descritivos (Conventional Commits: `feat:`, `fix:`, `test:`, `refactor:`).

## Comandos

```bash
./gradlew assembleDebug        # build de debug
./gradlew testDebugUnitTest    # testes unitários
./gradlew connectedAndroidTest # testes instrumentados (precisa de emulador/aparelho)
./gradlew lint                 # lint do Android
```

## Testes

- Toda regra do `domain` precisa de teste unitário, especialmente: leitura de OFX/CSV e de valores, divisão de parcelas, ciclo de fatura (compra antes/depois do fechamento, virada de mês e de ano), geração de recorrências (dia 31 em meses curtos, fevereiro), cálculo do "disponível" e do guardado por caixinha.
- Testes de DAO com Room em memória.
- Ao corrigir bug financeiro, primeiro escreva um teste que reproduz o erro.

## Roadmap

1. [x] Modelo de dados e camada `domain` com testes (Room já sobre SQLCipher)
2. [x] Cadastro rápido de lançamento (meta: 2–3 toques) e lista do mês
3. [x] Recorrências e parcelas
4. [x] Cartão de crédito e faturas
5. [x] Tela "quanto ainda posso gastar"
6. [x] SQLCipher, bloqueio biométrico, backup criptografado e exportação CSV
7. [x] Dinheiro guardado: caixinhas, aportes e resgates (sem rendimentos)
8. [x] Orçamento por categoria, gráficos e painel de assinaturas
9. [x] Importação de extrato (OFX/CSV)

Atualize os checkboxes conforme as etapas forem concluídas.

## O que NÃO fazer

- Não usar `Double`/`Float` para dinheiro.
- Não adicionar dependências proprietárias, de analytics ou que façam rede.
- Não colocar regra financeira em ViewModel ou Composable.
- Não fazer migração destrutiva do Room (`fallbackToDestructiveMigration`). Sempre escrever migrações.
- Não remover ou pular testes que falham; corrigir a causa.
