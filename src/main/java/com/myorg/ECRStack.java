package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryProps;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.constructs.Construct;

public class ECRStack extends Stack {

    private final Repository productServiceRepository;
    private final Repository auditServiceRepository;
    private final Repository invoiceServiceRepository;

    public ECRStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.productServiceRepository = new Repository(this, "ProductsService", RepositoryProps.builder()
            .repositoryName("productservice")
            .removalPolicy(RemovalPolicy.DESTROY)
            .imageTagMutability(TagMutability.IMMUTABLE)
            .autoDeleteImages(true)
            .build());

        this.auditServiceRepository = new Repository(this, "AuditService", RepositoryProps.builder()
            .repositoryName("auditservice")
            .removalPolicy(RemovalPolicy.DESTROY)
            .imageTagMutability(TagMutability.IMMUTABLE)
            .autoDeleteImages(true)
            .build());
        
        this.invoiceServiceRepository = new Repository(this, "InvoiceService", RepositoryProps.builder()
            .repositoryName("invoiceservice")
            .removalPolicy(RemovalPolicy.DESTROY)
            .imageTagMutability(TagMutability.IMMUTABLE)
            .autoDeleteImages(true)
            .build());
    
    }

    public Repository getProductServiceRepository() {
        return this.productServiceRepository;
    }

    public Repository getAuditServiceRepository() {
        return this.auditServiceRepository;
    }

    public Repository getInvoiceServiceRepository() {
        return this.invoiceServiceRepository;
    }

}
