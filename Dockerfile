FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY ManagementPlatform.slnx ./
COPY src/ManagementPlatform.Domain/ManagementPlatform.Domain.csproj src/ManagementPlatform.Domain/
COPY src/ManagementPlatform.Application/ManagementPlatform.Application.csproj src/ManagementPlatform.Application/
COPY src/ManagementPlatform.Infrastructure/ManagementPlatform.Infrastructure.csproj src/ManagementPlatform.Infrastructure/
COPY src/ManagementPlatform.Api/ManagementPlatform.Api.csproj src/ManagementPlatform.Api/

RUN dotnet restore src/ManagementPlatform.Api/ManagementPlatform.Api.csproj

COPY src/ src/
RUN dotnet publish src/ManagementPlatform.Api/ManagementPlatform.Api.csproj \
    --configuration Release \
    --output /app/publish \
    --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app

COPY --from=build /app/publish .

EXPOSE 8080
ENTRYPOINT ["dotnet", "ManagementPlatform.Api.dll"]
