<template>
  <div class="product-card" @click="$router.push(`/product/${product.id}`)">
    <div class="img-wrap">
      <img :src="product.mainImage" :alt="product.name" loading="lazy" />
    </div>
    <div class="info">
      <p class="name">{{ product.name }}</p>
      <p class="subtitle">{{ product.subtitle }}</p>
      <div class="bottom">
        <span class="price">
          <span class="symbol">¥</span>{{ product.price }}
        </span>
        <span class="sales">已售 {{ product.sales }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ product: { type: Object, required: true } })
</script>

<style scoped>
.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.img-wrap {
  width: 100%;
  aspect-ratio: 1;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info {
  padding: 10px 12px 14px;
}

.name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subtitle {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bottom {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 8px;
}

.price {
  color: #ff5000;
  font-size: 18px;
  font-weight: 600;
}

.symbol {
  font-size: 12px;
}

.sales {
  font-size: 12px;
  color: #999;
}
</style>
