#  Digital Bank API

Esta é uma API REST funcional, desenvolvida em **Spring Boot 3.x** e **Java 21**, projetada para simular um cenário de banco digital com transferências de saldo seguras, tratando problemas reais de **alta concorrência** e **tolerância a falhas na rede**.

---

## Decisões de Design e Arquitetura Adotadas

O desafio deste projeto não é fazer uma conta de menos em um saldo, mas sim garantir que o sistema não perca dados e não fique inconsistente quando centenas de requisições baterem no mesmo milissegundo ou quando a rede falhar. 
Abaixo estão as justificativas das tecnologias e padrões que escolhi:

### 1. O Dilema do Bloqueio: Uso do Lock Pessimista
Durante o desenho da solução, avaliei três caminhos para proteger o saldo dos clientes contra *Race Condition* (condição de corrida, onde duas requisições alteram o mesmo dado ao mesmo tempo):

* **Lock Otimista (`@Version`):** Evita travas no banco, mas se houver colisão (ex: várias transferências para a mesma conta no mesmo segundo), ele rejeita a transação e joga o erro para o cliente. Em um cenário financeiro de alta concorrência, o índice de falhas e reprocessamentos seria altíssimo.
* **Trava Distribuída com Redis :** É uma excelente idéia, mas traria uma complexidade operacional e de infraestrutura desnecessária para este escopo.
* **A Escolha: Lock Pessimista de Escrita (`FOR UPDATE`):** O PostgreSQL já possui um motor de travas nativo e extremamente rápido. O Lock Pessimista garante de forma estrita que a primeira requisição "tranca" a conta no banco e as outras esperam de forma ordenada por milissegundos. Nenhuma transação inválida sequer começa.

> **Prevenção de Deadlocks:** Se o Usuário A transferir para o B e o B transferir para o A no mesmo instante, os locks cruzados poderiam travar o banco de dados (*Deadlock*). 
Para resolver isso, criei uma **ordenação lógica por ID** no código antes de aplicar o lock. O sistema sempre bloqueia primeiro a conta com o menor ID alfanumérico, quebrando o ciclo de interdependência de forma nativa.

### 2. Garantia de Entrega: Padrão Transactional Outbox (https://docs.aws.amazon.com/pt_br/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)
Em requisitos Funcionais, item 'C' entendi que existe um evento do tipo push para o cliente. Para simular esse cenario utilizei o RabbitMQ para desacoplar o envio da mensagem do processamento em si.

* **O Problema:** Se o sistema atualizar o saldo no banco e logo em seguida tentar enviar a mensagem para a fila do RabbitMQ, uma piscada na rede ou uma queda do broker de mensageria faria a mensagem sumir, deixando o cliente sem notificação.
* **A Solução:** Adotei o padrão **Transactional Outbox**. Em vez de falar com o RabbitMQ direto no fluxo da transferência, o evento é gravada em uma tabela chamada `outbox_events` **dentro da mesma transação do banco de dados** que altera os saldos. Se o banco falhar, tudo sofre rollback. Se o banco persistir, o evento está salvo com segurança.

### 3. Escalabilidade do Agendador (Scheduler com `SKIP LOCKED`)
* **O Problema:** Para ler a tabela do Outbox e despachar para o RabbitMQ, usamos um agendador periódico (`@Scheduled`). Se subirmos 3 instâncias da aplicação para aguentar o tráfego, as três tentariam ler os mesmos registros pendentes ao mesmo tempo, gerando duplicidade de mensagens.
* **A Solução:** A query de lote usa o recurso **`SKIP LOCKED`** do PostgreSQL. Ele diz ao banco: *"Traga as primeiras 100 linhas pendentes, mas pule imediatamente as que já estiverem sendo processadas por outra instância"*. Com isso, nossa aplicação escala horizontalmente de forma leve e sem precisar de ferramentas de trava externa (como ShedLock).

### 4. Resiliência no Consumidor: Por que o Retry do Resilience4j?
* **O Problema:** O consumidor lê a mensagem da fila e tenta enviar a notificação (Push/Email) para o cliente. Se o gateway de envio estiver fora do ar naquele momento, a mensagem falharia e seria perdida ou jogada direto para a fila de erros (DLQ).
* **A Solução:** Utilizamos o **Retry do Resilience4j**. Ele aplica uma política de tentativas automáticas com espaçamento de tempo (ex: tentar novamente após 2 segundos). Isso resolve falhas momentâneas de rede (*glitches*) sem precisar descartar a mensagem, garantindo que oscilações temporárias de serviços terceiros não quebrem o fluxo do nosso banco.

### 5. Escolha das Ferramentas de Infraestrutura
* **Undertow (Servidor Web):** Substituí o Tomcat padrão pelo Undertow por sua arquitetura de I/O não bloqueante. Ele consome muito menos memória por conexão e entrega uma taxa de vazão (*throughput*) muito maior para APIs REST.
* **RabbitMQ (Broker):** Escolhido por ser extremamente leve, focado em filas em memória e possuir suporte nativo e maduro para roteamento flexível e *Dead Letter Exchanges (DLQ)*. O Apache Kafka adicionaria uma complexidade de logs distribuídos desnecessária para este caso.
* **Docker Compose:** Adotado para garantir que o ambiente rode exatamente igual na máquina de qualquer avaliador com um único comando.

---

## Estrutura do Repositório

O projeto está organizado da seguinte forma:

```text
banco-digital-cwi
│
├── ambiente/                   Arquivos de infraestrutura e banco
│   ├── docker-compose.yml      Orquestrador dos containers
│   └── init.sql                Script de tabelas e carga inicial de dados
│
└── banco-digital-cwi/          Raiz da aplicação Java (Spring Boot)
│   └── src/                    Código fonte do projeto
│   └── Dockerfile              Build multistage da imagem Java 21
│   └── pom.xml                 Gerenciador de dependências Maven
│	
└── postman/                    Collection do postman exportada
	
---

## Como Rodar o Projeto	

Pré-requisitos
Para a execução padrão do projeto, o único pré-requisito é ter o Docker e o Docker Compose instalados e rodando na sua máquina. 
Graças ao empacotamento em Multistage, não é necessário configurar o Java ou o Maven localmente.


Passo 1: Inicializar o Ecossistema Completo
Abra o seu terminal, navegue até a pasta dedicada ao ambiente e suba os containers:

Bash
dentro da pasta "ambiente" execute o comando abaixo. Será construida a imagem base da aplicação, junto com o rabbit e o PostgreSQL. 
docker compose up --build


---

## Links Úteis para Testes e Monitoramento

Assim que os logs do terminal indicarem que a API está de pé, os seguintes serviços estarão disponíveis:

Documentação Swagger (OpenAPI): http://localhost:8080/swagger-ui/index.html
Painel de Controle do RabbitMQ: http://localhost:15672
Usuário: bank_mq_user
Senha: bank_mq_password

---

## Como Testar (Collection do Postman Incluída)
Para facilitar a sua validação das regras de negócio e do comportamento sob falhas, disponibilizei os arquivos de testes prontos na pasta /postman 