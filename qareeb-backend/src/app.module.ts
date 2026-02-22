import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaModule } from './prisma/prisma.module';
import { TaskModule } from './task/task.module';
import { UserModule } from './user/user.module';
import { TransactionModule } from './transaction/transaction.module';

@Module({
  imports: [PrismaModule, TaskModule, UserModule, TransactionModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
