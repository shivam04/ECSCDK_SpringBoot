package com.myorg;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Environment;

public class ECommerceEcsCdkApp {
    public static void main(final String[] args) {
        App app = new App();
        Environment environment = Environment.builder()
            .account("390231347566")
            .region("ap-northeast-2")
            .build();
        
        Map<String, String> infraTags = new HashMap<>();
        infraTags.put("team", "ShvmsnhaCode");
        infraTags.put("cost", "ECommerceInfra");

        ECRStack ecrStack = new ECRStack(app, "Ecr", StackProps.builder()
            .env(environment)
            .tags(infraTags)
            .build());

        VPCStack vpcStack = new VPCStack(app, "Vpc", StackProps.builder()
            .env(environment)
            .tags(infraTags)
            .build());
        
        ClusterStack clusterStack = new ClusterStack(app, "Cluster", StackProps.builder()
            .env(environment)
            .tags(infraTags)
            .build(), new ClusterStackProps(vpcStack.getVpc()));
        clusterStack.addDependency(vpcStack);

        NlbStack nlbStack = new NlbStack(app, "LoadBalncer", StackProps.builder()
        .env(environment)
        .tags(infraTags)
        .build(), new NlbStackProps(vpcStack.getVpc()));
        nlbStack.addDependency(vpcStack);

        ApiStack apiStack = new ApiStack(app, "Api", StackProps.builder()
            .env(environment)
            .tags(infraTags)
            .build(), new ApiStackProps(nlbStack.getNetworkLoadBalancer(), nlbStack.getVpcLink()));
        
        apiStack.addDependency(nlbStack);

        Map<String, String> productsServiceTags = new HashMap<>();
        productsServiceTags.put("team", "ShvmsnhaCode");
        productsServiceTags.put("cost", "ProductService");

        ProductsServiceStack productServiceStack = new ProductsServiceStack(app, 
            "ProductsService", StackProps.builder()
                .env(environment)
                .tags(productsServiceTags)
                .build(), new ProductsServiceStackProps(
                    clusterStack.getCluster(), 
                    vpcStack.getVpc(), 
                    nlbStack.getNetworkLoadBalancer(), 
                    nlbStack.getApplicationLoadBalancer(), 
                    ecrStack.getProductServiceRepository()));

        productServiceStack.addDependency(vpcStack);
        productServiceStack.addDependency(clusterStack);
        productServiceStack.addDependency(nlbStack);
        productServiceStack.addDependency(ecrStack);
        productServiceStack.addDependency(apiStack);

        Map<String, String> auditServiceTags = new HashMap<>();
        auditServiceTags.put("team", "ShvmsnhaCode");
        auditServiceTags.put("cost", "AuditService");

        AuditServiceStack auditServiceStack = new AuditServiceStack(app, 
            "AuditService", StackProps.builder()
            .env(environment)
            .tags(auditServiceTags)
            .build(), new AuditServiceStackProps(
                clusterStack.getCluster(), 
                vpcStack.getVpc(), 
                nlbStack.getNetworkLoadBalancer(), 
                nlbStack.getApplicationLoadBalancer(),
                ecrStack.getAuditServiceRepository(), 
                productServiceStack.getProductEventsTopic()));
        
        auditServiceStack.addDependency(vpcStack);
        auditServiceStack.addDependency(clusterStack);
        auditServiceStack.addDependency(nlbStack);
        auditServiceStack.addDependency(ecrStack);
        auditServiceStack.addDependency(productServiceStack);
        auditServiceStack.addDependency(apiStack);

        Map<String, String> invoicesServiceTags = new HashMap<>();
        invoicesServiceTags.put("team", "ShvmsnhaCode");
        invoicesServiceTags.put("cost", "InvoicesService");

        InvoicesServiceStack invoicesServiceStack = new InvoicesServiceStack(app, 
            "InvoicesService", StackProps.builder()
            .env(environment)
            .tags(invoicesServiceTags)
            .build(), new InvoicesServiceStackProps(
                clusterStack.getCluster(), 
                vpcStack.getVpc(), 
                nlbStack.getNetworkLoadBalancer(), 
                nlbStack.getApplicationLoadBalancer(),
                ecrStack.getInvoiceServiceRepository()));
        invoicesServiceStack.addDependency(vpcStack);
        invoicesServiceStack.addDependency(clusterStack);
        invoicesServiceStack.addDependency(nlbStack);
        invoicesServiceStack.addDependency(ecrStack);
        invoicesServiceStack.addDependency(productServiceStack);
        invoicesServiceStack.addDependency(apiStack);
        
        app.synth();
    }
}

