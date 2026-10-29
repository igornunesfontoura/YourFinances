# Your Finances

App Android pessoal de controle financeiro: entradas, saídas fixas, saídas flexíveis e investimentos. Local-first, privado e open source (GPL-3.0). Uso inicial pessoal, mas o código deve estar pronto para publicação no F-Droid.

## Princípios inegociáveis

- **Local-first:** todos os dados ficam no aparelho. Não existe backend, conta de usuário nem nuvem.
- **Privacidade:** zero telemetria, analytics ou crash reporting. Nada de Firebase, Google Play Services, Crashlytics ou SDKs de terceiros que façam rede.
- **Única exceção de rede:** a API pública SGS do Banco Central, sem nenhum dado pessoal na requisição. Deve poder ser desligada nas configurações, e o app precisa funcionar 100% offline.
- **Compatível com F-Droid:** apenas dependências open source, builds reproduzíveis, nenhum blob proprietário.
- **Os dados são do usuário:** sempre deve existir exportação em CSV aberto, além do backup criptografado.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Single-Activity com Navigation Compose
- Room sobre SQLCipher (`net.zetetic:sqlcipher-android`); chave do banco protegida pelo Android Keystore
- Koin para injeção de dependência
- WorkManager para tarefas em segundo plano (recorrências, atualização de taxas)
- Ktor Client (ou OkHttp) somente para a API do BCB
- `androidx.biometric` para bloqueio; `FLAG_SECURE` em todas as telas
- Storage Access Framework para backup/exportação (sem permissão de armazenamento)
- Testes: JUnit 5 + kotlinx-coroutines-test; Turbine para Flows

Antes de adicionar qualquer dependência nova, confirme que é open source, sem Play Services e compatível com F-Droid. Na dúvida, pergunte.

## Arquitetura

```
app/
  data/      Room (entities, DAOs), SQLCipher, cliente da API do BCB, repositórios
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
Tipos: `INCOME` (salário, renda extra), `FIXED_EXPENSE` (assinaturas, contas, parcelas), `FLEXIBLE_EXPENSE` (jantar, roupa, cinema), `INVESTMENT` (aporte).

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

### Cartão de crédito
- Cada cartão tem dia de fechamento e dia de vencimento.
- Compra feita após o fechamento cai na fatura do mês seguinte.
- A fatura é a soma dos lançamentos daquele ciclo; seu pagamento é o evento de caixa.

### "Quanto ainda posso gastar" (tela principal)
```
disponível = entradas do mês
           − saídas fixas do mês (previstas + confirmadas)
           − meta de investimento do mês
           − saídas flexíveis já registradas
```
Este é o número mais importante do app. Qualquer mudança nessa regra exige teste.

### Investimentos
- Cadastro manual: produto, indexador (`CDI`, `SELIC`, `IPCA`, `PREFIXADO`), taxa, data e valor do aporte.
- Rendimento estimado com séries da API SGS do BCB:
  - CDI diário: série 12
  - Selic diária: série 11
  - IPCA mensal: série 433
  - Endpoint: `https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados?formato=json&dataInicial=dd/MM/yyyy&dataFinal=dd/MM/yyyy`
  - Verificar na documentação do BCB os limites de período por requisição.
- Pós-fixado em % do CDI: fator acumulado = produto, dia útil a dia útil, de `(1 + taxaDia/100 × percentual)`.
- Taxas ficam em cache local (Room). Sem rede, usar o cache e indicar a data da última atualização.
- IR estimado pela tabela regressiva de renda fixa. Sempre rotular como **estimativa**.
- Cálculos de rendimento usam `BigDecimal` internamente e só convertem para centavos (`Long`) no resultado.

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

- Toda regra do `domain` precisa de teste unitário, especialmente: divisão de parcelas, ciclo de fatura (compra antes/depois do fechamento, virada de mês e de ano), geração de recorrências (dia 31 em meses curtos, fevereiro), cálculo do "disponível" e rendimento por CDI.
- Testes de DAO com Room em memória.
- Ao corrigir bug financeiro, primeiro escreva um teste que reproduz o erro.

## Roadmap

1. [x] Modelo de dados e camada `domain` com testes (Room já sobre SQLCipher)
2. [x] Cadastro rápido de lançamento (meta: 2–3 toques) e lista do mês
3. [x] Recorrências e parcelas
4. [x] Cartão de crédito e faturas
5. [x] Tela "quanto ainda posso gastar"
6. [x] SQLCipher, bloqueio biométrico, backup criptografado e exportação CSV
7. [ ] Investimentos com taxas do BCB
8. [ ] Orçamento por categoria, gráficos e painel de assinaturas
9. [ ] Importação de extrato (OFX/CSV)

Atualize os checkboxes conforme as etapas forem concluídas.

## O que NÃO fazer

- Não usar `Double`/`Float` para dinheiro.
- Não adicionar dependências proprietárias, de analytics ou que façam rede.
- Não colocar regra financeira em ViewModel ou Composable.
- Não fazer migração destrutiva do Room (`fallbackToDestructiveMigration`). Sempre escrever migrações.
- Não remover ou pular testes que falham; corrigir a causa.
