# MAQPRO — catálogo de equipamentos

Aplicação Angular + Spring Boot com armazenamento em arquivo JSON local com identidade vermelha e preta baseada na arte fornecida. O código já está criado neste diretório; não é necessário copiar trechos ou criar arquivos manualmente.

## Executar localmente

Requisitos: Java 21, Maven 3.9+ e Node.js 24. Não é necessário instalar banco de dados.

Na raiz do projeto (se já existir `.env`, edite-o em vez de sobrescrevê-lo):

```bash
cp .env.example .env
```

Edite `.env`: defina `ADMIN_PASSWORD` com pelo menos 12 caracteres e `WHATSAPP_NUMBER` com país, DDD e telefone, apenas dígitos. O usuário administrativo padrão é `admin`, alterável por `ADMIN_USERNAME`. A senha administrativa precisa ter pelo menos 12 caracteres para o backend iniciar. `CATALOG_FILE` define onde salvar o catálogo; o padrão é `data/equipment.json`, relativo à pasta de execução do backend. Se um valor contiver espaços ou caracteres especiais do shell, coloque-o entre aspas simples.

O Spring Boot não carrega `.env` automaticamente. No Bash, carregue as variáveis antes de iniciar o backend, partindo da raiz do projeto:

```bash
set -a
source .env
set +a
cd backend
mvn spring-boot:run
```

Em outro terminal, partindo da raiz:

```bash
cd frontend
npm ci
npm start
```

- Site: http://localhost:4200
- Administração: http://localhost:4200/admin
- API: http://localhost:8080/api/equipment
- Login: usuário e senha configurados no `.env`.
- Encerrar: pressione `Ctrl+C` em cada terminal.

O proxy do Angular encaminha `/api` para o backend, mantendo a sessão na mesma origem. Os dados permanecem no arquivo JSON após encerrar as aplicações. Sem telefone configurado, o aplicativo informa isso ao solicitar orçamento.

O catálogo começa vazio: entre na administração e cadastre seus equipamentos reais. Imagens são cadastradas por URL HTTPS pública; este projeto não inclui upload de arquivos. A arte fornecida fica em `frontend/public/assets/maqpro.png`.

## Organização dos arquivos

```text
.
├── .env.example                  # Modelo da configuração privada
├── backend/
│   ├── pom.xml                   # Dependências Java e build Maven
│   └── src/
│       ├── main/java/br/com/maqpro/
│       │   ├── MaqproApplication.java
│       │   ├── config/SecurityConfig.java
│       │   ├── controller/       # Endpoints de catálogo, sessão e configuração
│       │   ├── service/EquipmentService.java
│       │   ├── repository/EquipmentRepository.java
│       │   ├── entity/Equipment.java
│       │   └── dto/EquipmentDto.java
│       ├── main/resources/
│       │   └── application.yml
│       └── test/java/br/com/maqpro/CatalogIntegrationTest.java
└── frontend/
    ├── package.json              # Dependências e comandos npm
    ├── angular.json              # Configuração do Angular
    ├── proxy.conf.json           # Proxy /api para desenvolvimento local
    ├── public/assets/maqpro.png
    ├── e2e/catalog.spec.ts        # Teste de navegação real com Playwright
    └── src/
        ├── main.ts               # Inicialização e locale pt-BR
        ├── styles.css            # Tema e regras responsivas
        └── app/
            ├── app.component.*   # Cabeçalho, rodapé e notificações
            ├── app.routes.ts     # Rotas e proteção da administração
            ├── models/equipment.ts
            ├── services/
            │   ├── api.service.ts
            │   └── cart.service.ts
            ├── components/product-card.component.*
            └── pages/
                ├── catalog.page.*
                ├── detail.page.*
                ├── cart.page.*
                ├── login.page.*
                └── admin.page.*
```

Cada página/componente possui um arquivo TypeScript e um template HTML. Controller recebe e valida requisições; Service implementa operações; Repository lê e grava o arquivo JSON; Entity representa um equipamento imutável; DTO define os dados expostos pela API.

## Funcionalidades

- Catálogo com foto, categoria, descrição, preço, busca, filtros e ordenação.
- Página de detalhes e seleção de quantidade (1 a 99 por equipamento).
- Carrinho persistido no navegador, alteração de quantidades, remoção e total em reais.
- Orçamento com itens, quantidades, preços unitários, subtotais e total; abertura do WhatsApp com texto codificado. O usuário confirma o envio no WhatsApp.
- Antes de gerar o orçamento, os itens são consultados novamente na API. Preços alterados e equipamentos excluídos exigem nova revisão do carrinho.
- Login e logout por sessão, cookie HttpOnly, proteção CSRF, rotas de escrita restritas a administrador e senha codificada com BCrypt.
- CRUD administrativo com validação no servidor, confirmação de exclusão e mensagens de erro.

O administrador único é configurado por variáveis de ambiente e carregado em memória com senha codificada. Os equipamentos são persistidos no arquivo JSON configurado. Não há cadastro de usuários, pagamento, controle de estoque ou armazenamento de pedidos; o carrinho serve para solicitar um orçamento, e os valores não incluem frete.

## Executar pelo IntelliJ

Importe `backend/pom.xml` como projeto Maven e selecione um SDK Java 21. Crie uma configuração de execução para `br.com.maqpro.MaqproApplication` e preencha suas variáveis de ambiente com os valores de `.env.example`, usando suas credenciais reais. Execute o frontend pelo terminal com `npm start` dentro de `frontend`.

O arquivo `.env` serve para a execução via Bash descrita acima; ao iniciar pelo IntelliJ, configure as variáveis na própria configuração de execução. Use `backend` como diretório de trabalho ou defina `CATALOG_FILE` com um caminho absoluto para acessar o mesmo catálogo em ambos os modos.

## API

| Método | Endpoint | Acesso |
| --- | --- | --- |
| GET | `/api/equipment` | Público |
| GET | `/api/equipment/{id}` | Público |
| POST | `/api/equipment` | Administrador + CSRF |
| PUT | `/api/equipment/{id}` | Administrador + CSRF |
| DELETE | `/api/equipment/{id}` | Administrador + CSRF |
| GET | `/api/config` | Público, número do WhatsApp |
| GET | `/api/auth/csrf` | Público, token e nome do cabeçalho |
| POST | `/api/auth/login` | Formulário username/password + CSRF |
| GET | `/api/auth/me` | Administrador autenticado |
| POST | `/api/auth/logout` | Administrador + CSRF |

Exemplo de corpo para criar/editar:

```json
{
  "name": "Leg Press 45°",
  "description": "Descrição e especificações do equipamento.",
  "price": 12500.00,
  "imageUrl": "https://seu-dominio.com/imagens/leg-press.jpg",
  "category": "Musculação"
}
```

## Verificação

```bash
cd backend
mvn verify
```

Os testes usam arquivos temporários isolados e verificam CRUD, validação, autorização, CSRF, login e logout. Os testes de armazenamento verificam persistência após reabrir o arquivo, IDs únicos, gravações concorrentes e preservação dos dados em caso de erro.

```bash
cd frontend
npm ci
npm run build
npx playwright install chromium
```

Com as aplicações em execução e o WhatsApp configurado, inicie o backend com `CATALOG_FILE` apontando para um arquivo separado de testes e rode:

```bash
E2E_ADMIN_PASSWORD='sua-senha-administrativa-forte' npm run test:e2e
```

O teste cria, edita e exclui um equipamento temporário, valida o carrinho após recarregar a página, intercepta a navegação para WhatsApp sem enviar mensagens e verifica a largura da página no celular. O endereço padrão é `http://localhost:4200`; defina `E2E_BASE_URL` se usar outro endereço. `E2E_ADMIN_USERNAME` permite alterar o usuário. Capturas são gravadas em `frontend/test-results/`.

## Publicação

Para executar o backend com Docker, rode na raiz do projeto, com o `.env` já configurado:

```bash
docker build -t maqpro-backend ./backend
docker run --rm --name maqpro-backend -p 8080:8080 \
  --env-file .env \
  -e CATALOG_FILE=/app/data/equipment.json \
  -v maqpro-data:/app/data \
  maqpro-backend
```

Ao usar `--env-file`, escreva os valores no formato `CHAVE=valor`, sem aspas externas e sem `export`. A imagem usa Java 21, executa os testes durante o build e inicia com usuário sem privilégios de root. O volume `maqpro-data` preserva o catálogo entre execuções; começa vazio e não importa `backend/data`. A API fica disponível em http://localhost:8080/api/equipment.

Gere o backend com `mvn package` dentro de `backend` e execute `java -jar target/maqpro-api-1.0.0.jar` com as variáveis de ambiente configuradas. Gere o frontend com `npm run build` dentro de `frontend` e sirva `frontend/dist/maqpro/browser` em um servidor web com fallback das rotas para `index.html` e encaminhamento de `/api` para a API.

Use HTTPS e defina `COOKIE_SECURE=true` nesse ambiente. Faça backup do arquivo configurado em `CATALOG_FILE` e use URLs estáveis para as fotografias.

## Armazenamento sem banco de dados

Ao executar a partir de `backend`, o arquivo padrão é `backend/data/equipment.json`. A pasta e o arquivo são criados no primeiro cadastro. Não crie um arquivo vazio manualmente. Se desejar outro local, configure `CATALOG_FILE` com um caminho absoluto.

O catálogo é carregado ao iniciar. Cada alteração grava um arquivo temporário na mesma pasta e substitui o JSON de forma atômica; uma falha de gravação não altera o catálogo em memória. IDs não são reutilizados após exclusões. Se o JSON estiver inválido, o backend interrompe a inicialização e preserva o arquivo para recuperação.

Use uma única instância do backend por arquivo. Não edite o JSON enquanto o backend estiver executando. Esse armazenamento é adequado para o catálogo local; não permite compartilhar o arquivo entre vários servidores. Não há importação automática de dados de uma instalação anterior que usava banco de dados.
