# CBR MOD — Android companion

Projeto Android nativo em Java. O aplicativo inclui login local de demonstração, abertura do Free Fire instalado e um painel flutuante independente com bolha arrastável e alternância por três toques.

## Credenciais locais de demonstração
- Usuário: criscbr
- Senha: 1234

## Build
O GitHub Actions executa `gradle assembleDebug` em cada push para `main` e publica o APK como artefato da execução.

## Limites funcionais
O painel não injeta código no Free Fire, não altera memória do jogo e não implementa aimbot, ESP ou mecanismos anti-ban. As categorias exibidas são identificadores de seção, não recursos ativos. O Android exige que o usuário conceda manualmente a permissão de sobreposição.
