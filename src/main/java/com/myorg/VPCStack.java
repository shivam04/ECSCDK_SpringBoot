package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;
import software.constructs.Construct;

public class VPCStack extends Stack {

    private final Vpc vpc;

    public VPCStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = new Vpc(this, "Vpc", VpcProps.builder()
            .vpcName("ECommerceVPC")
            .maxAzs(2)
            // DO NOT DO THIS IN PRODUCTION!!
            //.natGateways(0)
            .build());
    }

    public Vpc getVpc() {
        return this.vpc;
    }
}
