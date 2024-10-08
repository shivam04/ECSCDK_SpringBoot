package com.myorg;

import software.amazon.awscdk.services.apigateway.AccessLogFormat;
import software.amazon.awscdk.services.apigateway.ConnectionType;
import software.amazon.awscdk.services.apigateway.Integration;
import software.amazon.awscdk.services.apigateway.IntegrationOptions;
import software.amazon.awscdk.services.apigateway.IntegrationProps;
import software.amazon.awscdk.services.apigateway.IntegrationType;
import software.amazon.awscdk.services.apigateway.JsonSchema;
import software.amazon.awscdk.services.apigateway.JsonSchemaType;
import software.amazon.awscdk.services.apigateway.JsonWithStandardFieldProps;
import software.amazon.awscdk.services.apigateway.LogGroupLogDestination;
import software.amazon.awscdk.services.apigateway.MethodLoggingLevel;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.Model;
import software.amazon.awscdk.services.apigateway.ModelProps;
import software.amazon.awscdk.services.apigateway.RequestValidator;
import software.amazon.awscdk.services.apigateway.RequestValidatorProps;
import software.amazon.awscdk.services.apigateway.Resource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.apigateway.VpcLink;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public class ApiStack extends Stack {

    public ApiStack(final Construct scope, final String id, 
        final StackProps props, ApiStackProps apiStackProps) {
        super(scope, id, props);

        LogGroup logGroup = new LogGroup(this, "ECommerceApiLogs", LogGroupProps.builder()
            .logGroupName("ECommerceAPI")
            .removalPolicy(RemovalPolicy.DESTROY)
            .retention(RetentionDays.ONE_MONTH)
            .build());

        RestApi restApi = new RestApi(this, "RestApi", RestApiProps.builder()
            .restApiName("ECommerceAPI")
            .cloudWatchRole(true)
            .deployOptions(StageOptions.builder()
                .loggingLevel(MethodLoggingLevel.INFO)
                .accessLogDestination(new LogGroupLogDestination(logGroup))
                .accessLogFormat(
                    AccessLogFormat.jsonWithStandardFields(
                        JsonWithStandardFieldProps.builder()
                            .caller(true)
                            .httpMethod(true)
                            .ip(true)
                            .protocol(true)
                            .requestTime(true)
                            .requestTime(true)
                            .resourcePath(true)
                            .status(true)
                            .user(true)
                            .responseLength(true)
                            .build()
                    ) 
                )
                .build())
            .build());
        Resource productsResource = this.createProductsResource(restApi, apiStackProps);
        this.createProductEventsResource(restApi, apiStackProps, productsResource);
        this.createInvoicesResource(restApi, apiStackProps);
    }

    private void createInvoicesResource(RestApi restApi, ApiStackProps apiStackProps) {
        // POST /invoices
        Resource invoicesResource = restApi.getRoot().addResource("invoices");

        Map<String, String> invoicesIntegrationParameters = new HashMap<>();
        invoicesIntegrationParameters.put("integration.request.header.requestId", "context.requestId");

        Map<String, Boolean> invoicesMethodParameters = new HashMap<>();
        invoicesMethodParameters.put("method.request.header.requestId", false);

        RequestValidator invoiceRequestValidator = new RequestValidator(this, "InvoicesValidator", 
            RequestValidatorProps.builder()
            .restApi(restApi)
            .requestValidatorName("InvoicesValidator")
            .validateRequestParameters(true)
            .build()
        );

        invoicesResource.addMethod("POST", new Integration(
            IntegrationProps.builder()
            .type(IntegrationType.HTTP_PROXY)
            .integrationHttpMethod("POST")
            .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":9095/api/invoices")
            .options(
                IntegrationOptions.builder()
                .vpcLink(apiStackProps.vpcLink())
                .connectionType(ConnectionType.VPC_LINK)
                .requestParameters(invoicesIntegrationParameters)
                .build()
            )
            .build()), MethodOptions.builder()
            .requestValidator(invoiceRequestValidator)
            .requestParameters(invoicesMethodParameters)
            .build()
        );

        // GET /api/invoices/transactions/{fileTransactionId}
        Map<String, String> invoiceFileTransactionIntegrationParameters = new HashMap<>();
        invoiceFileTransactionIntegrationParameters.put("integration.request.header.requestId", "context.requestId");
        invoiceFileTransactionIntegrationParameters.put("integration.request.path.fileTransactionId", 
            "method.request.path.fileTransactionId");

        Map<String, Boolean> invoiceFileTransactionMethodParameters = new HashMap<>();
        invoiceFileTransactionMethodParameters.put("method.request.header.requestId", false);
        invoiceFileTransactionMethodParameters.put("method.request.path.fileTransactionId", true);

        Resource invoiceTransactionResource = invoicesResource.addResource("transactions");
        Resource invoiceFileTransactionResource = invoiceTransactionResource.addResource("{fileTransactionId}");

        RequestValidator invoiceTransactionsRequestValidator = new RequestValidator(this, "InvoiceTransactionsValidator", 
            RequestValidatorProps.builder()
            .restApi(restApi)
            .requestValidatorName("InvoiceTransactionsValidator")
            .validateRequestParameters(true)
            .build()
        );

        invoiceFileTransactionResource.addMethod("GET", new Integration(
            IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("GET")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":9095/api/invoices/transactions/{fileTransactionId}")
                .options(IntegrationOptions.builder()
                    .vpcLink(apiStackProps.vpcLink())
                    .connectionType(ConnectionType.VPC_LINK)
                    .requestParameters(invoiceFileTransactionIntegrationParameters)
                    .build())
                .build()), MethodOptions.builder()
                .requestParameters(invoiceFileTransactionMethodParameters)
                .requestValidator(invoiceTransactionsRequestValidator)
                .build()
        );

        // GET /api/invoices/?email=shivamtest@test.com

        Map<String, Boolean> customerInvoicesMethodParameters = new HashMap<>();
        customerInvoicesMethodParameters.put("method.request.header.requestId", false);
        customerInvoicesMethodParameters.put("method.request.querystring.email", true);

        RequestValidator customerRequestValidator = new RequestValidator(this, "CustomerValidator", 
            RequestValidatorProps.builder()
            .restApi(restApi)
            .requestValidatorName("CustomerValidator")
            .validateRequestParameters(true)
            .build()
        );

        invoicesResource.addMethod("GET", new Integration(
            IntegrationProps.builder()
            .type(IntegrationType.HTTP_PROXY)
            .integrationHttpMethod("GET")
            .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":9095/api/invoices")
            .options(
                IntegrationOptions.builder()
                .vpcLink(apiStackProps.vpcLink())
                .connectionType(ConnectionType.VPC_LINK)
                .requestParameters(invoicesIntegrationParameters)
                .build()
            )
            .build()), MethodOptions.builder()
            .requestValidator(customerRequestValidator)
            .requestParameters(customerInvoicesMethodParameters)
            .build()
        );

        
    }

    private void createProductEventsResource(RestApi restApi, ApiStackProps apiStackProps, Resource productsResource) {
        // /products/events
        Resource productsEventsResource = productsResource.addResource("events");

        Map<String, String> productEventsIntegrationParameters = new HashMap<>();
        productEventsIntegrationParameters.put("integration.request.header.requestId", "context.requestId");

        Map<String, Boolean> productEventsMethodParameters = new HashMap<>();
        productEventsMethodParameters.put("method.request.header.requestId", false);
        productEventsMethodParameters.put("method.request.querystring.eventType", true);
        productEventsMethodParameters.put("method.request.querystring.limit", false);
        productEventsMethodParameters.put("method.request.querystring.exclusiveStartTimestamp", false);
        productEventsMethodParameters.put("method.request.querystring.from", false);
        productEventsMethodParameters.put("method.request.querystring.to", false);

        RequestValidator productEventsRequestValidator = new RequestValidator(this, "ProductEventsRequestValidator", 
            RequestValidatorProps.builder()
                .restApi(restApi)
                .requestValidatorName("Product events request validator")
                .validateRequestParameters(true)
                .build()
        );
        // GET /products/events?eventType=PRODUCT_CREATED&limit=10&from=1&to=5&exclusiveStartTimestamp=123
        productsEventsResource.addMethod("GET", new Integration(
            IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("GET")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":9090/api/products/events")
                .options(IntegrationOptions.builder()
                    .vpcLink(apiStackProps.vpcLink())
                    .connectionType(ConnectionType.VPC_LINK)
                    .requestParameters(productEventsIntegrationParameters)
                    .build())
                .build()   
        ), MethodOptions.builder()
            .requestValidator(productEventsRequestValidator)
            .requestParameters(productEventsMethodParameters)
            .build());
    }

    private Resource createProductsResource(RestApi restApi, ApiStackProps apiStackProps) {
        Map<String, String> productsIntegrationParameters = new HashMap<>();
        productsIntegrationParameters.put("integration.request.header.requestId", "context.requestId");

        Map<String, Boolean> productsMethodParameters = new HashMap<>();
        productsMethodParameters.put("method.request.header.requestId", false);
        productsMethodParameters.put("method.request.querystring.code", false);

        // /products
        Resource productsResource = restApi.getRoot().addResource("products");

        //GET /products
        //GET /products?code=CODE1
        productsResource.addMethod("GET", new Integration(
            IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("GET")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":8080/api/products")
                .options(IntegrationOptions.builder()
                    .vpcLink(apiStackProps.vpcLink())
                    .connectionType(ConnectionType.VPC_LINK)
                    .requestParameters(productsIntegrationParameters)
                    .build())
                .build()   
        ), MethodOptions.builder()
            .requestParameters(productsMethodParameters)
            .build());

        RequestValidator productRequestValidator = new RequestValidator(this, "ProductRequestValidator", 
            RequestValidatorProps.builder()
                .restApi(restApi)
                .requestValidatorName("Product request validator")
                .validateRequestBody(true)
                .build()
        );

        Map<String, JsonSchema> productModelProperties = new HashMap<>();
        productModelProperties.put("name", JsonSchema.builder()
            .type(JsonSchemaType.STRING)
            .minLength(5)
            .maxLength(50)
            .build());

        productModelProperties.put("code", JsonSchema.builder()
            .type(JsonSchemaType.STRING)
            .minLength(5)
            .maxLength(15)
            .build());

        productModelProperties.put("model", JsonSchema.builder()
            .type(JsonSchemaType.STRING)
            .minLength(5)
            .maxLength(50)
            .build());

        productModelProperties.put("price", JsonSchema.builder()
            .type(JsonSchemaType.NUMBER)
            .minimum(10.0)
            .maximum(1000.0)
            .build());

        Model productModel = new Model(this, "ProductModel", 
            ModelProps.builder()
                .modelName("ProductModel")
                .restApi(restApi)
                .contentType("application/json")
                .schema(JsonSchema.builder()
                    .type(JsonSchemaType.OBJECT)
                    .properties(productModelProperties)
                    .required(Arrays.asList("name", "code"))
                    .build())
                .build()
        );

        Map<String, Model> productRequestModel = new HashMap<>();
        productRequestModel.put("application/json", productModel);

        // POST /products
        productsResource.addMethod("POST", new Integration(
            IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("POST")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":8080/api/products")
                .options(IntegrationOptions.builder()
                    .vpcLink(apiStackProps.vpcLink())
                    .connectionType(ConnectionType.VPC_LINK)
                    .requestParameters(productsIntegrationParameters)
                    .build())
                .build()   
        ), MethodOptions.builder()
            .requestParameters(productsMethodParameters)
            .requestValidator(productRequestValidator)
            .requestModels(productRequestModel)
            .build());

        // PUT /products/{id}
        Map<String, String> productIdIntegrationParamteres = new HashMap<>();
        productIdIntegrationParamteres.put("integration.request.path.id", "method.request.path.id");
        productIdIntegrationParamteres.put("integration.request.header.requestId", "context.requestId");

        Map<String, Boolean> productIdMethodParamteres = new HashMap<>();
        productIdMethodParamteres.put("method.request.path.id", true);
        productIdMethodParamteres.put("method.request.header.requestId", false);

        Resource productIdResource = productsResource.addResource("{id}");
        productIdResource.addMethod("PUT", new Integration(
            IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("PUT")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":8080/api/products/{id}")
                .options(IntegrationOptions.builder()
                    .vpcLink(apiStackProps.vpcLink())
                    .connectionType(ConnectionType.VPC_LINK)
                    .requestParameters(productIdIntegrationParamteres)
                    .build())
                .build()), MethodOptions.builder()
                .requestParameters(productIdMethodParamteres)
                .requestValidator(productRequestValidator)
                .requestModels(productRequestModel)
                .build()
        );

        // GET /products/{id}
        productIdResource.addMethod("GET", new Integration(
            IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("GET")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":8080/api/products/{id}")
                .options(IntegrationOptions.builder()
                    .vpcLink(apiStackProps.vpcLink())
                    .connectionType(ConnectionType.VPC_LINK)
                    .requestParameters(productIdIntegrationParamteres)
                    .build())
                .build()), MethodOptions.builder()
                .requestParameters(productIdMethodParamteres)
                .build()
        );

        // DELETE /products/{id}
        productIdResource.addMethod("DELETE", new Integration(
            IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("DELETE")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + 
                    ":8080/api/products/{id}")
                .options(IntegrationOptions.builder()
                    .vpcLink(apiStackProps.vpcLink())
                    .connectionType(ConnectionType.VPC_LINK)
                    .requestParameters(productIdIntegrationParamteres)
                    .build())
                .build()), MethodOptions.builder()
                .requestParameters(productIdMethodParamteres)
                .build()
        );

        return productsResource;
    }
}


record ApiStackProps(
    NetworkLoadBalancer networkLoadBalancer,
    VpcLink vpcLink
) {}