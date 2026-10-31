# Your Finances

App Android de finanças pessoais que mostra o número mais importante do mês: **quanto ainda posso gastar**. Registra entradas, saídas fixas e flexíveis, cartões de crédito, parcelamentos, recorrências e dinheiro guardado em caixinhas, tudo **só no seu aparelho**: sem internet, sem conta, sem anúncios e sem telemetria.

<!-- DEMO: substitua a linha abaixo por um GIF do app em uso -->
<p align="center"><em>🎬 GIF de demonstração em breve</em></p>

## Por que existe

Eu queria um app de finanças com tudo o que eu precisava, sem entregar meus dados a ninguém.

## Funcionalidades

- **Quanto ainda posso gastar:** a tela principal desconta das entradas do mês as saídas fixas, a meta de investimento e o que já foi gasto, e mostra a parte do mês já comprometida
- **Lançamentos:** cadastro rápido de entradas, saídas fixas, saídas flexíveis, aportes e resgates, com lista do mês por competência ou por caixa
- **Recorrências:** salário, contas e assinaturas geram lançamentos previstos que você confirma ou ajusta, inclusive salário pago em duas datas no mês
- **Cartão de crédito:** faturas por ciclo, respeitando o dia de fechamento e o de vencimento de cada cartão
- **Parcelamentos:** cada parcela cai no mês certo, e o app mostra quando cada compra termina e quanto do futuro já está comprometido
- **Dinheiro guardado:** caixinhas com objetivo, aportes, resgates e meta mensal de investimento
- **Orçamento e relatórios:** categorias com limite mensal, gráficos e painel de assinaturas
- **Importação de extrato:** arquivos OFX e CSV do banco ou do cartão, com revisão antes de salvar e detecção de lançamentos repetidos
- **Seus dados são seus:** backup criptografado com senha sua e exportação dos lançamentos em CSV aberto

## Como funciona

Os dados ficam num banco **Room** criptografado com **SQLCipher**. A chave do banco é protegida pelo **Android Keystore**, então nem quem copiar os arquivos do app consegue lê-los. O app abre com biometria ou com a senha do aparelho, e as telas são protegidas contra captura.

O app não pede a permissão de internet e não usa Google Play Services nem SDKs de terceiros. Backup e exportação usam o seletor de arquivos do Android, sem permissão de armazenamento.

As regras financeiras ficam numa camada em Kotlin puro, coberta por testes unitários. Os valores são guardados em centavos, sem ponto flutuante, para nunca sumir um centavo nas contas.

Feito em **Kotlin** com **Jetpack Compose** e Material 3.

## Requisitos

- Celular com **Android 8.0** ou mais novo
- Para compilar: **JDK 17** e o **Android SDK 35** (o Android Studio já instala os dois)

## Instalação

Baixe o APK da [última release](https://github.com/igornunesfontoura/YourFinances/releases/latest) e abra no celular. O Android vai pedir para permitir a instalação de apps dessa fonte. As notas de cada release trazem o SHA-256 do APK e do certificado de assinatura, para você conferir.

Para compilar a partir do código, com o celular conectado por USB e a depuração USB ativada:

```sh
git clone https://github.com/igornunesfontoura/YourFinances.git
cd YourFinances
./gradlew installDebug
```

Pelo Android Studio, abra a pasta do projeto e clique em **Run**.

Para rodar os testes:

```sh
./gradlew testDebugUnitTest      # testes unitários
./gradlew connectedAndroidTest   # testes instrumentados (emulador ou aparelho)
```

## Uso

1. Na primeira abertura, o app apresenta as telas de boas-vindas. Os guias ficam depois em **Mais › Como usar**.
2. Cadastre as recorrências (salário, aluguel, assinaturas) e os cartões de crédito. O mês já começa com as saídas fixas previstas.
3. Ao longo do mês, lance os gastos do dia a dia ou importe o extrato do banco. O "Você ainda pode gastar" se atualiza na hora.
4. De tempos em tempos, faça um backup em **Mais › Configurações › Fazer backup criptografado** e guarde o arquivo e a senha fora do celular. Sem a senha, não há como recuperar.

## Limitações

- **O backup é manual.** Não há backup automático nem lembrete. Se você trocar de aparelho, redefinir o bloqueio de tela ou restaurar o sistema, os dados só voltam a partir de um backup.
- O app registra só o que foi guardado, não os rendimentos, porque eles dependem de cada banco e produto. O saldo real se confere no banco.
- Moeda única: real (BRL).
- Ainda não está no F-Droid.

## Licença

[GPL-3.0](LICENSE)
