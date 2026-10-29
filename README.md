# Your Finances

App Android de controle financeiro pessoal: entradas, saídas fixas e flexíveis, cartões de crédito, parcelamentos, recorrências e dinheiro guardado em caixinhas. Mostra o número mais importante do mês: **quanto ainda posso gastar**.

## Privacidade

- Todos os dados ficam no aparelho, num banco criptografado (SQLCipher, chave no Android Keystore).
- O app não usa internet, não tem conta, nuvem, anúncios, telemetria nem Google Play Services.
- Bloqueio por biometria ou senha do aparelho; telas protegidas contra captura.
- Backup criptografado com senha sua e exportação dos lançamentos em CSV aberto.

## Recursos

- Lançamento rápido, lista do mês por competência ou caixa.
- Recorrências (inclusive no cartão), compras parceladas, faturas de cartão.
- Meta mensal de investimento, aportes e resgates por caixinha.
- Categorias com limite mensal, relatórios e painel de assinaturas.
- Importação de extratos OFX e CSV, com revisão e detecção de duplicados.

## Compilar

Requer JDK 17 e Android SDK 35.

```bash
./gradlew assembleDebug        # APK de debug
./gradlew testDebugUnitTest    # testes unitários
./gradlew connectedAndroidTest # testes instrumentados (emulador ou aparelho)
```

## Licença

GPL-3.0. Veja [LICENSE](LICENSE).
