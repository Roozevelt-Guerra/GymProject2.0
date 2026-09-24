# MAQPRO — catálogo de equipamentos

Aplicação Angular + Spring Boot com persistência em PostgreSQL via Spring Data JPA com identidade vermelha e preta baseada na arte fornecida. O código já está criado neste diretório; não é necessário copiar trechos ou criar arquivos manualmente.

## Executar localmente

Requisitos: Java 21, Maven 3.9+ e Node.js 24. É necessário PostgreSQL acessível ao backend (local ou no Render).

Na raiz do projeto (se já existir `.env`, edite-o em vez de sobrescrevê-lo):

```bash
cp .env.example .env
```

Edite `.env`: defina `ADMIN_PASSWORD` com pelo menos 12 caracteres e `WHATSAPP_NUMBER` com país, DDD e telefone, apenas dígitos. O usuário administrativo padrão é `admin`, alterável por `ADMIN_USERNAME`. A senha administrativa precisa ter pelo menos 12 caracteres para o backend iniciar. `DATABASE_URL` deve usar o formato JDBC (`jdbc:postgresql://localhost:5432/maqpro`), sem credenciais na URL; preencha `DATABASE_USERNAME` e `DATABASE_PASSWORD`. Crie previamente o banco `maqpro` e um usuário com permissão de criar tabelas e sequências nesse banco. `CATALOG_FILE` aponta para o JSON legado a importar; o padrão é `data/equipment.json`, relativo à pasta de execução do backend. Com `CATALOG_IMPORT_ENABLED=true` (padrão), esse arquivo é obrigatório até a primeira importação concluída. Para uma instalação nova sem JSON, defina `CATALOG_IMPORT_ENABLED=false`. Se um valor contiver espaços ou caracteres especiais do shell, coloque-o entre aspas simples.

O Spring Boot não carrega `.env` automaticamente. Os comandos abaixo carregam as variáveis no Bash a partir da raiz do projeto.

Se ainda não possui PostgreSQL local, uma opção é Docker. Preencha primeiro `DATABASE_USERNAME` e `DATABASE_PASSWORD` no `.env`, mantenha a URL local indicada acima e execute na raiz:

```bash
set -a
source .env
set +a
docker run -d --name maqpro-postgres \
  -e POSTGRES_DB=maqpro \
  -e POSTGRES_USER="$DATABASE_USERNAME" \
  -e POSTGRES_PASSWORD="$DATABASE_PASSWORD" \
  -p 127.0.0.1:5432:5432 \
  -v maqpro-postgres-data:/var/lib/postgresql/data postgres:17
```

O volume mantém os dados. Nas execuções seguintes use `docker start maqpro-postgres`. As credenciais acima inicializam apenas volumes novos. Para executar o backend:

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

O proxy do Angular encaminha `/api` para o backend, mantendo a sessão na mesma origem. Os dados permanecem no PostgreSQL após encerrar as aplicações. Sem telefone configurado, o aplicativo informa isso ao solicitar orçamento.

Na primeira execução, o catálogo existente é importado do JSON. Sem importação, entre na administração e cadastre seus equipamentos reais. Imagens são cadastradas por URL HTTPS pública; este projeto não inclui upload de arquivos. A arte fornecida fica em `frontend/public/assets/maqpro.png`.

## Organização dos arquivos

```text
.
├── .env.example                  # Modelo da configuração privada
├── backend/
│   ├── pom.xml                   # Dependências Java e build Maven
│   └── src/
│       ├── main/java/br/com/maqpro/
│       │   ├── MaqproApplication.java
│       │   ├── config/           # Segurança e importação na inicialização
│       │   ├── controller/       # Endpoints de catálogo, sessão e configuração
│       │   ├── service/          # EquipmentService e CatalogImporter
│       │   ├── repository/       # EquipmentRepository e CategoryRepository
│       │   ├── entity/           # Equipment e Category
│       │   └── dto/EquipmentDto.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/V1__create_catalog.sql
│       └── test/java/br/com/maqpro/ # API, persistência e importação com PostgreSQL
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

Cada página/componente possui um arquivo TypeScript e um template HTML. Controller recebe e valida requisições; Service implementa operações; Repository usa Spring Data JPA; Equipment e Category são entidades relacionadas por `ManyToOne`; DTO mantém os campos consumidos pelo Angular. O Flyway cria e versiona o esquema e o Hibernate apenas o valida (`ddl-auto=validate`).

## Funcionalidades

- Catálogo com foto, categoria, descrição, preço, busca, filtros e ordenação.
- Página de detalhes e seleção de quantidade (1 a 99 por equipamento).
- Carrinho persistido no navegador, alteração de quantidades, remoção e total em reais.
- Orçamento com itens, quantidades, preços unitários, subtotais e total; abertura do WhatsApp com texto codificado. O usuário confirma o envio no WhatsApp.
- Antes de gerar o orçamento, os itens são consultados novamente na API. Preços alterados e equipamentos excluídos exigem nova revisão do carrinho.
- Login e logout por sessão, cookie HttpOnly, proteção CSRF, rotas de escrita restritas a administrador e senha codificada com BCrypt.
- CRUD administrativo com validação no servidor, confirmação de exclusão e mensagens de erro.

O administrador único é configurado por variáveis de ambiente e carregado em memória com senha codificada. Os equipamentos e suas categorias são persistidos no PostgreSQL. Não há cadastro de usuários, pagamento, controle de estoque ou armazenamento de pedidos; o carrinho serve para solicitar um orçamento, e os valores não incluem frete.

## Executar pelo IntelliJ

Importe `backend/pom.xml` como projeto Maven e selecione um SDK Java 21. Crie uma configuração de execução para `br.com.maqpro.MaqproApplication` e preencha suas variáveis de ambiente com os valores de `.env.example`, usando suas credenciais reais. Execute o frontend pelo terminal com `npm start` dentro de `frontend`.

O arquivo `.env` serve para a execução via Bash descrita acima; ao iniciar pelo IntelliJ, configure as variáveis na própria configuração de execução. Use `backend` como diretório de trabalho ou defina `CATALOG_FILE` com um caminho absoluto para importar o mesmo JSON em ambos os modos. A conexão com o banco também deve ser configurada nas variáveis da execução.

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

Os testes iniciam PostgreSQL real temporário com `embedded-postgres`, sem Docker e sem acessar seu banco ou `.env`. Execute como usuário comum (o PostgreSQL não inicia como root). A primeira execução baixa os binários via Maven. São verificados CRUD, categorias, concorrência, validação, autorização, CSRF, login/logout, importação única, rollback e preservação do JSON. O Dockerfile também executa os testes com usuário comum durante o build.

```bash
cd frontend
npm ci
npm run build
npx playwright install chromium
```

Com as aplicações em execução e o WhatsApp configurado, use um **banco separado de testes**, `CATALOG_IMPORT_ENABLED=false` e as credenciais administrativas de teste; então rode:

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

Ao usar `--env-file`, escreva os valores no formato `CHAVE=valor`, sem aspas externas e sem `export`. A imagem usa Java 21, executa os testes durante o build e inicia com usuário sem privilégios de root. O banco precisa estar acessível pela URL configurada (dentro do contêiner, `localhost` aponta para o próprio contêiner). O volume só fornece o JSON legado: coloque nele o arquivo antes da primeira inicialização, ou desative a importação se o banco for novo. A imagem não inclui `backend/data` nem `.env`. A API fica disponível em http://localhost:8080/api/equipment.

Gere o backend com `mvn package` dentro de `backend` e execute `java -jar target/maqpro-api-1.0.0.jar` com as variáveis de ambiente configuradas. Gere o frontend com `npm run build` dentro de `frontend` e sirva `frontend/dist/maqpro/browser` em um servidor web com fallback das rotas para `index.html` e encaminhamento de `/api` para a API.

Use HTTPS e defina `COOKIE_SECURE=true` nesse ambiente. Mantenha backups do PostgreSQL e do JSON original da migração e use URLs estáveis para as fotografias.

## Migração segura do catálogo existente

1. Antes de substituir o backend antigo, suspenda cadastros/edições/exclusões e faça uma cópia do `equipment.json` **mais recente**. Se os dados foram alterados no Render, use o arquivo do Render, não uma cópia local desatualizada. Não apague nem sobrescreva o original.
2. Crie um banco PostgreSQL vazio. Configure as três variáveis `DATABASE_*` e `CATALOG_FILE` apontando para a cópia. Mantenha `CATALOG_IMPORT_ENABLED=true`.
3. Inicie o backend novo. O Flyway cria `category`, `equipment`, `catalog_import` e seu histórico. A importação valida todo o arquivo, preserva IDs/preços/URLs, compartilha categorias de mesmo nome (após remover espaços nas extremidades) e respeita `nextId`, inclusive IDs anteriormente excluídos. Itens legados recebem `active=true`.
4. Confira o log `Importação do catálogo concluída: N equipamentos` e as consultas abaixo. Só depois libere as alterações administrativas no backend novo.

A importação inteira e o registro de conclusão são transacionais. Um bloqueio no PostgreSQL impede importações duplicadas por instâncias simultâneas. JSON inválido/ausente, erro de banco ou banco já preenchido sem registro de importação interrompem a inicialização; corrija a causa e tente novamente. O arquivo nunca é alterado. Lacunas em sequências após rollback são normais e não perdem dados.

Depois de concluída, a importação é ignorada mesmo que o JSON mude, seja removido ou todos os equipamentos sejam excluídos. **Não apague o registro em `catalog_import` para forçar reimportação.** `CATALOG_IMPORT_ENABLED=false` desativa a leitura do JSON; não transforma um banco ocupado em candidato à importação.

O campo opcional `active` foi acrescentado ao DTO. Cadastros sem ele ficam ativos; edições sem ele preservam o valor. Para preservar o comportamento atual, os endpoints continuam listando todos os equipamentos e DELETE continua excluindo fisicamente. Não foi acrescentado filtro de ativos nem controle visual no Angular.

## PostgreSQL no Render

No painel Render, use **New → Postgres**, escolha nome, banco/usuário e uma região igual à do backend. Aguarde ficar disponível. Use host, porta, database, username e password exibidos em **Connect/Info**. Consulte a [documentação oficial de criação e conexão](https://render.com/docs/postgresql-creating-connecting).

Em **backend → Environment**, configure:

| Variável | Valor |
| --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://HOST_INTERNO:5432/NOME_DO_BANCO` |
| `DATABASE_USERNAME` | Usuário exibido pelo Render |
| `DATABASE_PASSWORD` | Senha exibida pelo Render, como segredo |
| `ADMIN_USERNAME` | Mantenha o usuário atual |
| `ADMIN_PASSWORD` | Mantenha a senha atual (mínimo 12 caracteres) |
| `WHATSAPP_NUMBER` | Mantenha o número atual |
| `COOKIE_SECURE` | `true` para HTTPS |
| `CATALOG_IMPORT_ENABLED` | `true` na migração; pode ser `false` após conferência |
| `CATALOG_FILE` | Caminho do JSON no processo que fará a importação |

**A URL deve ser JDBC.** Não cole diretamente `postgresql://usuario:senha@host/banco`: monte `jdbc:postgresql://host:porta/banco` e informe usuário/senha nas variáveis separadas. Para conexões externas use o host externo e acrescente `?sslmode=require`. Entre backend e banco no Render, use a conexão interna na mesma região, conforme a documentação oficial.

Como alternativa a enviar o JSON ao contêiner Render, execute **uma vez localmente** o JAR novo com a conexão externa do banco Render, `CATALOG_FILE` absoluto e importação habilitada. Pare essa execução após conferir os dados. Depois publique o backend usando a conexão interna para **o mesmo banco**: ele encontrará o registro da importação e não precisará do JSON. Suspenda gravações no backend antigo durante todo esse procedimento. Não coloque o JSON nem credenciais no Git/imagem Docker.

O frontend Vercel e seu encaminhamento atual de `/api` continuam iguais. Carrinho continua no navegador e orçamentos do WhatsApp não são gravados.

## Conferir a persistência no PostgreSQL

Conecte pelo comando PSQL fornecido pelo Render ou, localmente, com `psql -h localhost -U SEU_USUARIO -d maqpro -W`. Use o cliente do banco para executar:

```sql
SELECT e.id, e.name, e.price, e.image_url, c.name AS category, e.active
FROM equipment e JOIN category c ON c.id = e.category_id
ORDER BY e.id;

SELECT * FROM catalog_import;
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

Cadastre um equipamento na administração e confira a linha no SQL. Edite preço/categoria, consulte novamente, reinicie o backend e confirme que a API retorna os mesmos dados (`http://localhost:8080/api/equipment`). Exclua o item e confirme a ausência via SQL. Reinicie novamente: o JSON antigo não deve restaurar o equipamento excluído. Compare `imported_count` com a quantidade do JSON no momento da migração; esse contador histórico não muda com novos cadastros.

## Dependências adicionadas

- `spring-boot-starter-data-jpa`: entidades, repositórios e transações.
- `postgresql` (runtime): driver JDBC.
- `flyway-core` e `flyway-database-postgresql` (este em runtime): esquema SQL versionado, conforme a [integração do Spring Boot](https://docs.spring.io/spring-boot/how-to/data-initialization.html).
- `io.zonky.test:embedded-postgres:2.2.2` (test): PostgreSQL temporário real, conforme o [projeto oficial](https://github.com/zonkyio/embedded-postgres).
